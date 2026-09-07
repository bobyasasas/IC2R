package ic2.neoforge.client;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.item.ElectricItem;
import ic2.neoforge.item.ElectricItemEnergy;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

public record ChargeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<ChargeProperty> CODEC = MapCodec.unit(new ChargeProperty());

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        return stack.getItem() instanceof ElectricItem item
                ? (float) (ElectricItemEnergy.charge(stack) / item.specification().capacity())
                : 0;
    }

    @Override
    public MapCodec<ChargeProperty> type() {
        return CODEC;
    }
}
