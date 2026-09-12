package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.core.machine.RotorOperation;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.WindGeneratorBlockEntity;
import ic2.neoforge.machine.WindTurbineBlockEntity;
import ic2.neoforge.machine.WorldWind;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Legacy ItemWindMeter: an electric hand-held anemometer (10000 EU, tier 1, 50 EU per reading).
 * A bare use reads the wind at the player's height; using it on a wind generator or wind turbine
 * reports the effective wind there including rotor obstruction.
 */
public final class WindMeterItem extends ElectricItem {
    public static final double OPERATION_ENERGY_COST = 50;

    private static final double GENERATOR_OBSTRUCTION_FACTOR = 567.0;

    public WindMeterItem(Properties properties) {
        super(properties, new ElectricItemSpec(10000, 100, 1, false));
    }

    private static float roundWind(double windStrength) {
        return (float) Math.round(windStrength * 100.0) / 100.0F;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        tooltip.accept(Component.translatable("ic2.wind_meter.tooltipA"));
        tooltip.accept(Component.translatable("ic2.wind_meter.tooltipB"));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player == null || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof WindTurbineBlockEntity turbine) {
            boolean active = level.getBlockState(pos).getValue(MachineBlock.ACTIVE);
            RotorOperation operation = turbine.operation();
            if (!active) {
                if (operation.status() != RotorOperation.Status.NO_ROTOR) {
                    player.sendSystemMessage(
                            Component.translatable("ic2.wind_meter.info.rotor.blocked"));
                } else {
                    player.sendSystemMessage(
                            Component.translatable("ic2.wind_meter.info.rotor.none"));
                }
                return InteractionResult.FAIL;
            }
            ElectricItemEnergy.use(stack, OPERATION_ENERGY_COST, player);
            // Legacy maps a negative obstruction count to a fully blocked rotor plane.
            if (operation.status() == RotorOperation.Status.INTERFERENCE) {
                player.sendSystemMessage(
                        Component.translatable(
                                "ic2.wind_meter.info.blocked", operation.diameter() * 3));
            } else {
                float displayWind = roundWind(operation.environment());
                if (displayWind <= 0.0F) {
                    player.sendSystemMessage(
                            Component.translatable(
                                    "ic2.wind_meter.info.obstructed", operation.obstructions()));
                } else {
                    player.sendSystemMessage(
                            Component.translatable("ic2.wind_meter.info.effective", displayWind));
                }
            }
            return InteractionResult.SUCCESS;
        } else if (tile instanceof WindGeneratorBlockEntity generator) {
            ElectricItemEnergy.use(stack, OPERATION_ENERGY_COST, player);
            double wind = WorldWind.get((ServerLevel) level).windAt((ServerLevel) level, pos.getY());
            double obstructiveFactor = generator.obstructions() / GENERATOR_OBSTRUCTION_FACTOR;
            double effective = obstructiveFactor >= 1.0 ? 0.0 : wind * (1.0 - obstructiveFactor);
            float displayWind = roundWind(effective);
            if (displayWind <= 0.0F) {
                player.sendSystemMessage(
                        Component.translatable(
                                "ic2.wind_meter.info.obstructed", generator.obstructions()));
            } else {
                player.sendSystemMessage(
                        Component.translatable("ic2.wind_meter.info.effective", displayWind));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !player.level().equals(level)) {
            return InteractionResult.PASS;
        }
        if (!ElectricItemEnergy.use(stack, OPERATION_ENERGY_COST, player)) {
            return InteractionResult.PASS;
        }
        double windStrength =
                WorldWind.get((ServerLevel) level).windAt((ServerLevel) level, player.getBlockY());
        if (windStrength < 0.0) {
            windStrength = 0.0;
        }
        player.sendSystemMessage(
                Component.translatable("ic2.wind_meter.info", roundWind(windStrength)));
        return InteractionResult.SUCCESS;
    }
}
