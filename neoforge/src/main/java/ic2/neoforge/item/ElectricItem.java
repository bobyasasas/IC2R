package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ElectricItem extends Item {
    private final ElectricItemSpec specification;

    public ElectricItem(Properties properties, ElectricItemSpec specification) {
        super(properties);
        this.specification = specification;
    }

    public final ElectricItemSpec specification() {
        return specification;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(ElectricItemEnergy.charge(stack) / specification.capacity() * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(
                (float) (ElectricItemEnergy.charge(stack) / specification.capacity() / 3), 1, 1);
    }
}
