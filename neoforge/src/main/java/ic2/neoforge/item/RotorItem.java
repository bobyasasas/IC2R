package ic2.neoforge.item;

import ic2.core.machine.RotorMaterial;

import net.minecraft.world.item.Item;

public final class RotorItem extends Item {
    private final RotorMaterial material;

    public RotorItem(Properties properties, RotorMaterial material) {
        super(properties.durability(material.durability()));
        this.material = material;
    }

    public RotorMaterial material() {
        return material;
    }
}
