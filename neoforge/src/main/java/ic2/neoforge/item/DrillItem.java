package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Electric drill mining pickaxe- and shovel-mineable blocks while charged. Speed collapses to the
 * unpowered rate once the stored EU no longer covers one operation, like legacy. The vanilla ×5
 * underwater/airborne mining penalty is softened to the legacy ×3/5, and each break announces the
 * legacy hard/soft drill sound to the miner alone.
 */
public class DrillItem extends ElectricItem {
    private final float miningSpeed;
    private final double operationEnergyCost;
    private final double harvestEnergyCost;
    private final int minerEnergyPerTick;
    private final int minerDuration;
    private final int fortuneLevel;

    public DrillItem(
            Properties properties,
            ElectricItemSpec specification,
            TagKey<Block> incorrectForDrops,
            float miningSpeed,
            double operationEnergyCost,
            double harvestEnergyCost,
            int minerEnergyPerTick,
            int minerDuration,
            int fortuneLevel,
            float attackDamage) {
        super(
                properties.component(DataComponents.TOOL, tool(incorrectForDrops, miningSpeed))
                        .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes(attackDamage)),
                specification);
        this.miningSpeed = miningSpeed;
        this.operationEnergyCost = operationEnergyCost;
        this.harvestEnergyCost = harvestEnergyCost;
        this.minerEnergyPerTick = minerEnergyPerTick;
        this.minerDuration = minerDuration;
        this.fortuneLevel = fortuneLevel;
    }

    /** Drill tool rules from a tool material, extended to both pickaxe and shovel blocks. */
    private static Tool tool(TagKey<Block> incorrectForDrops, float speed) {
        HolderGetter<Block> blocks =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(blocks.getOrThrow(incorrectForDrops)),
                        Tool.Rule.minesAndDrops(
                                blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), speed),
                        Tool.Rule.minesAndDrops(
                                blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), speed)),
                1.0F,
                0,
                true);
    }

    /** Legacy DiggerItem attributes: the drill hits like its tier material at -3 attack speed. */
    private static ItemAttributeModifiers attributes(float attackDamage) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("ic2", "drill_attack_damage"),
                                attackDamage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath("ic2", "drill_attack_speed"),
                                -3.0,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Legacy getBreakSoundForBlock: hard material from destroy time 3.0 up, soft below. */
    public static Holder<SoundEvent> breakSoundFor(BlockState state) {
        return state.getBlock().defaultDestroyTime() >= 3.0F
                ? ModSounds.ITEM_DRILL_HARD
                : ModSounds.ITEM_DRILL_SOFT;
    }

    /**
     * Legacy getDestroySpeed penalties: a working drill triples its speed underwater (without
     * aqua affinity) and in the air, so the vanilla fifth stays a soft ×3/5.
     */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!(stack.getItem() instanceof DrillItem drill)
                || drill.getDestroySpeed(stack, event.getState()) == 1.0F) {
            return;
        }
        Player player = event.getEntity();
        if (player.getFluidInteraction()
                        .isEyeInFluidMatching(player, (_, type, _) -> type.getIsWaterLike())
                && player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) < 1.0F) {
            event.setNewSpeed(event.getNewSpeed() * 3.0F);
        }
        if (!player.onGround()) {
            event.setNewSpeed(event.getNewSpeed() * 3.0F);
        }
    }

    public boolean canUse(ItemStack stack) {
        return ElectricItemEnergy.charge(stack) >= operationEnergyCost;
    }

    /** Legacy drops require an effective block: pickaxe- or shovel-mineable, not energy-bound. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return super.isCorrectToolForDrops(stack, state)
                && (state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        || state.is(BlockTags.MINEABLE_WITH_SHOVEL));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return speed == 1.0F || !canUse(stack) ? 1.0F : this.miningSpeed;
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && canUse(stack)) {
            ElectricItemEnergy.use(stack, operationEnergyCost, owner);
        }
        if (owner instanceof ServerPlayer server) {
            server.connection.send(
                    new ClientboundSoundEntityPacket(
                            breakSoundFor(state),
                            server.getSoundSource(),
                            server,
                            1.0F,
                            1.0F,
                            level.getRandom().nextLong()));
        }
        return true;
    }

    /** EU consumed by the miner for one harvest through this drill. */
    public double harvestEnergyCost() {
        return harvestEnergyCost;
    }

    /** EU per tick the miner spends drilling one block with this drill. */
    public int minerEnergyPerTick() {
        return minerEnergyPerTick;
    }

    /** Ticks the miner spends drilling one block with this drill. */
    public int minerDuration() {
        return minerDuration;
    }

    /** Fortune level the miner applies to drops, matching the legacy iridium bonus. */
    public int fortuneLevel() {
        return fortuneLevel;
    }
}
