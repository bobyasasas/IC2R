package ic2.neoforge.item;

import ic2.core.machine.SolarGeneration;
import ic2.neoforge.component.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Legacy ItemArmorSolarHelmet and ItemArmorStaticBoots inventory ticks: the helmet tops up whatever
 * electric piece is worn on the chest from sunlight (the solar generator's brightness formula at
 * the player's position, one EU per full brightness), and the boots do the same from walking —
 * five blocks of horizontal travel per EU, three EU at most, riding or swimming freezing the
 * walk markers.
 */
public final class UtilityArmorHelper {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof SolarHelmetItem) {
            double brightness =
                    SolarGeneration.brightness(
                            level.dimensionType().hasSkyLight(),
                            level.getBrightness(
                                    LightLayer.SKY, BlockPos.containing(player.position())),
                            level.environmentAttributes()
                                    .getValue(
                                            EnvironmentAttributes.SUN_ANGLE,
                                            BlockPos.containing(player.position())),
                            false,
                            level.getRainLevel(1),
                            level.getThunderLevel(1));
            if (brightness > 0) {
                ElectricItemEnergy.charge(chest, brightness, Integer.MAX_VALUE, true, false);
            }
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.getItem() instanceof StaticBootsItem) {
            staticTick(player, boots, chest);
        }
    }

    private static void staticTick(Player player, ItemStack boots, ItemStack chest) {
        BlockPos pos = BlockPos.containing(player.position());
        boolean notWalking = player.isPassenger() || player.isInWater();
        if (notWalking || !boots.has(ModDataComponents.STATIC_BOOTS_X.get())) {
            boots.set(ModDataComponents.STATIC_BOOTS_X.get(), pos.getX());
        }
        if (notWalking || !boots.has(ModDataComponents.STATIC_BOOTS_Z.get())) {
            boots.set(ModDataComponents.STATIC_BOOTS_Z.get(), pos.getZ());
        }

        int lastX = boots.getOrDefault(ModDataComponents.STATIC_BOOTS_X.get(), pos.getX());
        int lastZ = boots.getOrDefault(ModDataComponents.STATIC_BOOTS_Z.get(), pos.getZ());
        double distance = Math.sqrt((double) (lastX - pos.getX()) * (lastX - pos.getX())
                + (double) (lastZ - pos.getZ()) * (lastZ - pos.getZ()));
        if (distance >= 5.0) {
            boots.set(ModDataComponents.STATIC_BOOTS_X.get(), pos.getX());
            boots.set(ModDataComponents.STATIC_BOOTS_Z.get(), pos.getZ());
            ElectricItemEnergy.charge(chest, Math.min(3.0, distance / 5.0), Integer.MAX_VALUE, true, false);
        }
    }

    private UtilityArmorHelper() {}
}
