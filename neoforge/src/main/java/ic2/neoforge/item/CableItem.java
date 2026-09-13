package ic2.neoforge.item;

import ic2.core.energy.VoltageTier;
import ic2.core.energy.grid.EnergyMode;
import ic2.neoforge.energy.CableBlock;
import ic2.neoforge.energy.EnergyConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Cable BlockItem carrying the legacy voltage and loss tooltip (legacy ItemCable). */
public class CableItem extends BlockItem {
    public CableItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag) {
        if (!(getBlock() instanceof CableBlock cable)) return;
        var spec = cable.specification();
        if (EnergyConfig.MODE.get() == EnergyMode.GT) {
            var tier = VoltageTier.fromPower(spec.voltageLimit());
            tooltip.accept(
                    Component.translatable(
                            "ic2.electric.tooltip.cable.max_voltage",
                            Component.translatable(tier.getTranslationKey()).getString()
                                    + " ("
                                    + spec.voltageLimit()
                                    + " V)"));
            tooltip.accept(
                    Component.translatable(
                            "ic2.electric.tooltip.cable.max_amperage", spec.ampLimit()));
            tooltip.accept(
                    Component.translatable("ic2.electric.tooltip.cable.loss", spec.gtLoss()));
        } else {
            tooltip.accept(
                    Component.translatable("item.ic2.cable.tooltip0", spec.voltageLimit()));
            tooltip.accept(
                    Component.translatable("item.ic2.cable.tooltip1", spec.classicLoss()));
        }
    }
}
