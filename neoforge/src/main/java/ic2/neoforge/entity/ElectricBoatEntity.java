package ic2.neoforge.entity;

import ic2.neoforge.item.ElectricItemEnergy;
import ic2.neoforge.registration.ModItems;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Legacy ElectricBoatEntity: drops itself, survives falls, fire immune, and burns 4 EU per tick
 * drawn from any worn electric armour piece while a player drives it. Powered it moves at 1.5×
 * block speed, unpowered it crawls at 0.25×. A lava bath leaves the boat unharmed but sets the
 * rider on fire for five seconds.
 */
public class ElectricBoatEntity extends Boat {
    private static final double POWER_COST = 4.0;
    /** Legacy scanned Inventory.armor; 26.1.2 stores those in the four humanoid armour slots. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private boolean hasPower = false;

    public ElectricBoatEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> ModItems.ELECTRIC_BOAT.get());
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    protected float getBlockSpeedFactor() {
        return super.getBlockSpeedFactor() * (this.hasPower ? 1.5F : 0.25F);
    }

    @Override
    public void tick() {
        this.hasPower = false;
        if (this.getControllingPassenger() instanceof Player player) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (ElectricItemEnergy.discharge(stack, POWER_COST, Integer.MAX_VALUE, true, true, true)
                        == POWER_COST) {
                    ElectricItemEnergy.discharge(
                            stack, POWER_COST, Integer.MAX_VALUE, true, true, false);
                    this.hasPower = true;
                    break;
                }
            }
        }

        super.tick();

        if (!this.level().isClientSide() && this.isInLava()) {
            Entity rider = this.getControllingPassenger();
            if (rider != null) {
                rider.igniteForSeconds(5.0F);
            }
        }
    }
}
