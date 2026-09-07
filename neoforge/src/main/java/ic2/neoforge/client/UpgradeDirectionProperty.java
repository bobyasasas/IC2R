package ic2.neoforge.client;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.item.UpgradeItem;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

public record UpgradeDirectionProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<UpgradeDirectionProperty> CODEC =
            MapCodec.unit(new UpgradeDirectionProperty());

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        var direction = UpgradeItem.direction(stack);
        return direction == null ? 0 : direction.ordinal() + 1;
    }

    @Override
    public MapCodec<UpgradeDirectionProperty> type() {
        return CODEC;
    }
}
