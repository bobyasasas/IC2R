package ic2.core.energy.grid;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.profile.IElectricalNode;
import ic2.api.energy.profile.VoltageTier;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.energy.profile.ElectricalProfile;

public final class ElectricalNodes {
    private ElectricalNodes() {}

    public static IElectricalNode resolve(IEnergyTile tile) {
        return tile instanceof IElectricalNode node ? node : null;
    }

    public static double getBufferFill(IElectricalNode node) {
        return node.getEnergyBufferCapacity() - node.getEnergyBufferFree();
    }

    public static int getGtOfferAmps(IEnergySource source) {
        double offered = source.getOfferedEnergy();
        if (offered <= 0.0) {
            return 0;
        }

        IElectricalNode node = resolve(source);
        if (node != null) {
            int voltage = node.getWorkingVoltage().getVoltage();
            return voltage > 0 && !(offered < voltage)
                    ? Math.min(node.getMaxSourceAmperage(), (int) Math.floor(offered / voltage))
                    : 0;
        }

        int tier = source.getSourceTier();
        if (tier < 0) {
            return 0;
        }

        int voltage = (int) EnergyNet.instance.getPowerFromTier(tier);
        return voltage > 0 && !(offered < voltage) ? (int) Math.floor(offered / voltage) : 0;
    }

    public static int getGtDemandAmps(IEnergySink sink) {
        double demanded = sink.getDemandedEnergy();
        if (demanded <= 0.0) {
            return 0;
        }

        IElectricalNode node = resolve(sink);
        if (node != null) {
            int voltage = node.getSinkWorkingVoltage().getVoltage();
            if (voltage <= 0) {
                return 0;
            }

            int fromBuffer = (int) Math.floor(node.getEnergyBufferFree() / voltage);
            return fromBuffer <= 0 ? 0 : Math.min(node.getMaxSinkAmperage(), fromBuffer);
        } else {
            int tier = sink.getSinkTier();
            if (tier < 0) {
                return 0;
            }

            int voltage = (int) EnergyNet.instance.getPowerFromTier(tier);
            return voltage > 0 ? (int) Math.floor(demanded / voltage) : 0;
        }
    }

    public static double getPacketPower(IEnergyTile tile, int packetIndex) {
        if (tile instanceof IEnergySource source) {
            return getSourcePacketPower(source, packetIndex);
        } else {
            return tile instanceof IEnergySink sink ? getSinkInjectVoltage(sink) : 0.0;
        }
    }

    public static double getMaxOfferPower(IEnergySource source, int packetCount) {
        if (packetCount <= 0) {
            return 0.0;
        } else {
            IElectricalNode node = resolve(source);
            if (node == null) {
                int tier = source.getSourceTier();
                return tier < 0 ? 0.0 : EnergyNet.instance.getPowerFromTier(tier) * packetCount;
            } else {
                int ampsPerPacket = getSourceAmpsPerPacket(node, source);
                return ampsPerPacket * node.getWorkingVoltage().getVoltage() * packetCount;
            }
        }
    }

    static double getInjectTierParameter(IEnergySink sink, double amount) {
        IElectricalNode node = resolve(sink);
        double powerForTier = node != null ? node.getSinkWorkingVoltage().getVoltage() : amount;
        return EnergyNet.instance.getTierFromPower(powerForTier);
    }

    private static double getSourcePacketPower(IEnergySource source, int packetIndex) {
        IElectricalNode node = resolve(source);
        if (node == null) {
            int tier = source.getSourceTier();
            return tier < 0 ? 0.0 : EnergyNet.instance.getPowerFromTier(tier);
        } else {
            int ampsPerPacket = getSourceAmpsPerPacket(node, source);
            return ampsPerPacket * node.getWorkingVoltage().getVoltage();
        }
    }

    private static double getSinkInjectVoltage(IEnergySink sink) {
        IElectricalNode node = resolve(sink);
        if (node != null) {
            return node.getSinkWorkingVoltage().getVoltage();
        }

        int tier = sink.getSinkTier();
        return tier < 0 ? 0.0 : EnergyNet.instance.getPowerFromTier(tier);
    }

    private static int getSourceAmpsPerPacket(IElectricalNode node, IEnergySource source) {
        if (source instanceof IMultiEnergySource multi && multi.sendMultipleEnergyPackets()) {
            return 1;
        } else {
            int workingCurrent = node.getWorkingCurrent();
            return workingCurrent > 0 ? workingCurrent : Math.max(1, node.getMaxSourceAmperage());
        }
    }

    static boolean validateIcModePacketCaps() {
        ElectricalProfile maceratorLike = new ElectricalProfile(VoltageTier.LV);
        maceratorLike.setRecipePower(2);
        int packetPower =
                maceratorLike.getWorkingCurrent() * maceratorLike.getWorkingVoltage().getVoltage();
        if (packetPower != 32) {
            return false;
        }

        ElectricalProfile lvGenerator = new ElectricalProfile(VoltageTier.LV);
        lvGenerator.setRecipePower(10);
        return lvGenerator.getWorkingCurrent() * lvGenerator.getWorkingVoltage().getVoltage() == 32;
    }
}
