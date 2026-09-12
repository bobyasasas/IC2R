package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModCells;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.function.Supplier;

/** A 1000 mB cell. Fluid identity is either fixed by its item ID or stored as a component. */
public final class FluidCellItem extends Item {
    private final Supplier<? extends Fluid> fluid;

    public FluidCellItem(Properties properties, Supplier<? extends Fluid> fluid) {
        super(properties);
        this.fluid = fluid;
    }

    public Fluid fixedFluid() {
        return fluid.get();
    }

    // Legacy ItemClassicCell: a drained cell comes back as the empty cell. An already-empty
    // cell (facade_cell without contents) is consumed as itself, so it stays a plain ingredient.
    @Override
    public ItemStackTemplate getCraftingRemainder(ItemInstance stack) {
        boolean empty = fixedFluid() == Fluids.EMPTY && stack.get(ModDataComponents.FLUID) == null;
        return empty ? null : new ItemStackTemplate(ModCells.EMPTY.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        var stored = stack.get(ModDataComponents.FLUID);
        if (fixedFluid() == Fluids.EMPTY && stored != null) {
            return Component.translatable(
                    "ic2.item.fluid_cell.contents",
                    stored.fluid().value().getFluidType().getDescription());
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.PASS;
        return FluidUtil.interactWithFluidHandler(
                        player,
                        context.getHand(),
                        context.getLevel(),
                        context.getClickedPos(),
                        context.getClickedFace(),
                        null)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var access = ItemAccess.forPlayerInteraction(player, hand).oneByOne();
        var handler = access.getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return InteractionResult.PASS;
        boolean empty = handler.getResource(0).isEmpty();
        var hit =
                getPlayerPOVHitResult(
                        level,
                        player,
                        empty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand)))
            return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var moved =
                empty
                        ? FluidUtil.tryPickupFluid(
                                handler, player, level, pos, hit.getDirection(), null)
                        : FluidUtil.tryPlaceFluid(
                                handler,
                                player,
                                level,
                                pos.relative(hit.getDirection()),
                                true,
                                null);
        return moved.isEmpty() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }
}
