package ic2.neoforge.client;

import com.mojang.serialization.MapCodec;

import ic2.neoforge.menu.ToolboxMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

public record ToolboxOpenProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<ToolboxOpenProperty> CODEC =
            MapCodec.unit(new ToolboxOpenProperty());

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        var player = Minecraft.getInstance().player;
        return player != null
                        && player.containerMenu instanceof ToolboxMenu menu
                        && menu.contains(stack)
                ? 1
                : 0;
    }

    @Override
    public MapCodec<ToolboxOpenProperty> type() {
        return CODEC;
    }
}
