package ic2.neoforge.machine;

import ic2.neoforge.entity.NukeEntity;
import ic2.neoforge.menu.NukeMenu;
import ic2.neoforge.registration.BalanceConfig;
import ic2.neoforge.registration.ModExplosives;
import ic2.neoforge.registration.ModMaterialBlocks;
import ic2.neoforge.registration.ModNuke;
import ic2.neoforge.registration.ModReactorItems;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;

import org.jspecify.annotations.Nullable;

/**
 * The nuke charge block (legacy TileEntityExplosive/TileEntityBridgeNuke/TileEntityNuke). One slot
 * consumes industrial TNT, one a radioactive booster; the loaded stack sizes feed the legacy
 * cube-root power formula. It primes like the industrial TNT: redstone, fire, burning arrows and
 * neighbouring explosions, but breaking it stays harmless and drops the charge.
 */
public final class NukeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int OUTSIDE_SLOT = 0;
    public static final int INSIDE_SLOT = 1;

    private final MachineInventory inventory =
            new MachineInventory(2, this::setChanged, NukeBlockEntity::accepts);
    private int radiationRange;
    private boolean exploded;

    public NukeBlockEntity(BlockPos pos, BlockState state) {
        super(ModNuke.NUKE_ENTITY.get(), pos, state);
    }

    public MachineInventory inventory() {
        return this.inventory;
    }

    public static boolean accepts(int slot, ItemResource resource) {
        Item item = resource.getItem();
        return switch (slot) {
            case OUTSIDE_SLOT -> item == ModExplosives.ITNT.get().asItem();
            case INSIDE_SLOT -> isRadioactive(item);
            default -> false;
        };
    }

    private static boolean isRadioactive(Item item) {
        return item == ModReactorItems.URANIUM_238.get()
                || item == ModReactorItems.URANIUM_235.get()
                || item == ModReactorItems.SMALL_URANIUM_235.get()
                || item == ModReactorItems.PLUTONIUM.get()
                || item == ModReactorItems.SMALL_PLUTONIUM.get()
                || item == ModMaterialBlocks.MATERIALS.get("uranium_block").get().asItem();
    }

    public int getRadiationRange() {
        return this.radiationRange;
    }

    public boolean isExploded() {
        return this.exploded;
    }

    private void setRadiationRange(int range) {
        this.radiationRange = range;
    }

    /**
     * Legacy TileEntityNuke.getNukeExplosivePower: cube-root TNT scaling plus boosts,
     * config-capped.
     */
    public float getNukeExplosivePower() {
        ItemStack outside = this.inventory.stack(OUTSIDE_SLOT);
        if (outside.isEmpty()) {
            return -1.0F;
        }

        int itntCount = outside.getCount();
        double ret = 5.0 * Math.pow(itntCount, 0.3333333333333333);
        ItemStack inside = this.inventory.stack(INSIDE_SLOT);
        if (inside.isEmpty()) {
            this.setRadiationRange(0);
        } else {
            Item insideItem = inside.getItem();
            int insideCount = inside.getCount();
            if (insideItem == ModReactorItems.URANIUM_238.get()) {
                this.setRadiationRange(itntCount);
            } else if (insideItem
                    == ModMaterialBlocks.MATERIALS.get("uranium_block").get().asItem()) {
                this.setRadiationRange(itntCount * 6);
            } else if (insideItem == ModReactorItems.SMALL_URANIUM_235.get()) {
                this.setRadiationRange(itntCount * 2);
                if (itntCount >= 64) {
                    ret += 0.05555555555555555 * Math.pow(insideCount, 1.6);
                }
            } else if (insideItem == ModReactorItems.URANIUM_235.get()) {
                this.setRadiationRange(itntCount * 2);
                if (itntCount >= 32) {
                    ret += 0.5 * Math.pow(insideCount, 1.4);
                }
            } else if (insideItem == ModReactorItems.SMALL_PLUTONIUM.get()) {
                this.setRadiationRange(itntCount * 3);
                if (itntCount >= 32) {
                    ret += 0.05555555555555555 * Math.pow(insideCount, 2.0);
                }
            } else if (insideItem == ModReactorItems.PLUTONIUM.get()) {
                this.setRadiationRange(itntCount * 4);
                if (itntCount >= 16) {
                    ret += 0.5 * Math.pow(insideCount, 1.8);
                }
            }
        }

        ret = Math.min(ret, BalanceConfig.NUKE_EXPLOSION_POWER_LIMIT.get());
        return (float) ret;
    }

    /** Called by the block hooks for redstone, fire, arrows and neighbour explosions. */
    public boolean explode(@Nullable LivingEntity igniter, boolean shortFuse) {
        if (this.exploded) {
            return true;
        }
        NukeEntity entity = this.createCharge(igniter);
        if (entity == null) {
            return false;
        }
        if (!(this.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return true;
        }

        this.exploded = true;
        this.onIgnite();
        serverLevel.removeBlock(this.worldPosition, false);
        if (shortFuse) {
            int fuse = entity.getFuse();
            entity.setFuse(serverLevel.getRandom().nextInt(Math.max(1, fuse / 4)) + fuse / 8);
        }
        serverLevel.addFreshEntity(entity);
        serverLevel.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return true;
    }

    private @Nullable NukeEntity createCharge(@Nullable LivingEntity igniter) {
        if (!BalanceConfig.ENABLE_NUKE.get()) {
            return null;
        }
        float power = this.getNukeExplosivePower();
        if (power < 0.0F) {
            return null;
        }
        return new NukeEntity(
                this.level,
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5,
                power,
                this.radiationRange,
                igniter);
    }

    /** Legacy TileEntityNuke.onIgnite: priming consumes the loaded stacks. */
    private void onIgnite() {
        this.inventory.set(OUTSIDE_SLOT, ItemResource.EMPTY, 0);
        this.inventory.set(INSIDE_SLOT, ItemResource.EMPTY, 0);
    }

    /** Chain-reaction entry from NukeBlock.onBlockExploded; true when the charge was primed. */
    public boolean onExploded(Explosion explosion) {
        if (this.exploded) {
            return true;
        }
        Entity source = explosion.getIndirectSourceEntity();
        return this.explode(source instanceof LivingEntity living ? living : null, true);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.inventory.copyToList());
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inventory.deserialize(input.childOrEmpty("inventory"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.inventory.serialize(output.child("inventory"));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new NukeMenu(id, playerInventory, this);
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.ic2.nuke");
    }
}
