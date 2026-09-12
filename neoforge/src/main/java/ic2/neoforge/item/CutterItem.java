package ic2.neoforge.item;

import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.energy.FoamCableBlock;
import ic2.neoforge.registration.MaterialDefinition;
import ic2.neoforge.registration.ModItems;
import ic2.neoforge.registration.ModMachines;
import ic2.neoforge.registration.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class CutterItem extends CraftingToolItem {
    public CutterItem(Properties properties) {
        super(properties);
    }

    private static CableBlock variant(CableBlock cable, int delta) {
        var spec = cable.specification();
        return ModMachines.CABLES.values().stream()
                .map(holder -> holder.get())
                .filter(
                        other ->
                                other.material() == cable.material()
                                        && other.specification().insulation()
                                                == spec.insulation() + delta)
                .findFirst()
                .orElse(null);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = level.getBlockState(pos);
        if (player == null || !(state.getBlock() instanceof CableBlock cable))
            return InteractionResult.PASS;
        var replacement = variant(cable, 1);
        if (replacement == null || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var inventory = PlayerInventoryWrapper.of(player);
        try (var transaction = Transaction.openRoot()) {
            if (inventory.extract(
                            ItemResource.of(
                                    ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get()),
                            1,
                            transaction)
                    != 1) return InteractionResult.PASS;
            if (!level.setBlockAndUpdate(pos, replacement.withPropertiesOf(state)))
                return InteractionResult.FAIL;
            transaction.commit();
        }
        context.getItemInHand()
                .hurtAndBreak(
                        1,
                        player,
                        context.getHand() == InteractionHand.MAIN_HAND
                                ? EquipmentSlot.MAINHAND
                                : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    public static void strip(Player player, Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()
                || !level.mayInteract(player, pos)
                || !(player.getMainHandItem().getItem() instanceof CutterItem)) return;
        if (state.getBlock() instanceof CableBlock cable) {
            var replacement = variant(cable, -1);
            if (replacement == null
                    || !level.setBlockAndUpdate(pos, replacement.withPropertiesOf(state))) return;
        } else if (state.getBlock() instanceof FoamCableBlock foam) {
            // Hardened foam shields the insulation and a bare foam cable has no layer to cut.
            if (!state.getValue(FoamCableBlock.FOAM).isSoft()
                    || foam.specification().insulation() <= 0) return;
            // Legacy routed the cut through the plain-cable types table, so the shell is lost
            // along with one insulation layer instead of revealing the same insulated cable.
            var replacement = variant(foam, -1);
            if (replacement == null
                    || !level.setBlockAndUpdate(
                            pos, CableBlock.connectedState(replacement, level, pos))) return;
        } else {
            return;
        }
        player.getMainHandItem().hurtAndBreak(3, player, EquipmentSlot.MAINHAND);
        Block.popResource(
                level, pos, new ItemStack(ModItems.MATERIALS.get(MaterialDefinition.RUBBER).get()));
        level.playSound(null, pos, ModSounds.ITEM_CUTTER_USE.get(), SoundSource.BLOCKS, 1, 1);
    }

    private static CableBlock variant(FoamCableBlock foam, int delta) {
        return ModMachines.CABLES.values().stream()
                .map(holder -> holder.get())
                .filter(
                        other ->
                                other.material() == foam.material()
                                        && other.specification().insulation()
                                                == foam.specification().insulation() + delta)
                .findFirst()
                .orElse(null);
    }
}
