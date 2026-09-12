package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.machine.MachineKind;
import ic2.neoforge.transfer.MachineInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

public final class UpgradeItem extends Item {
    public enum Kind {
        OVERCLOCKER,
        TRANSFORMER,
        ENERGY_STORAGE,
        EJECTOR,
        PULLING,
        FLUID_EJECTOR,
        FLUID_PULLING,
        REDSTONE_INVERTER,
        REMOTE_INTERFACE;

        public boolean directional() {
            return this == EJECTOR || this == PULLING || fluid();
        }

        public boolean fluid() {
            return this == FLUID_EJECTOR || this == FLUID_PULLING;
        }

        public boolean pulling() {
            return this == PULLING || this == FLUID_PULLING;
        }

        public boolean suitable(MachineKind machine) {
            // Legacy RedstoneSensitive machines (ItemUpgradeModule.isSuitableFor) — checked before
            // the per-machine cases below, which only enumerate the directional kinds. The port
            // magnetizer has no upgrade slots (pending parity), and the port replicator reads no
            // redstone yet, mirroring legacy where TileEntityReplicator declares the property but
            // wires no Redstone component, so the inverter is inert there in both codebases.
            if (this == REDSTONE_INVERTER)
                return machine == MachineKind.ADV_MINER
                        || machine == MachineKind.BLAST_FURNACE
                        || machine == MachineKind.CENTRIFUGE
                        || machine == MachineKind.INDUCTION_FURNACE
                        || machine == MachineKind.MATTER_GENERATOR
                        || machine == MachineKind.REPLICATOR;
            // Legacy has no RemotelyAccessible machine and InvSlotUpgrade.getRemoteRange has no
            // caller: the remote interface is an inert item there, so it stays unsuitable here.
            if (this == REMOTE_INTERFACE) return false;
            if (machine == MachineKind.STEAM_KINETIC_GENERATOR) return fluid() || this == PULLING;
            // Legacy cropmatron: transformer/energy storage/item consuming/fluid consuming.
            if (machine == MachineKind.CROPMATRON)
                return this == TRANSFORMER || this == ENERGY_STORAGE || pulling();
            // Legacy chunk loader properties: energy storage/transformer only (no processing).
            if (machine == MachineKind.CHUNK_LOADER)
                return this == TRANSFORMER || this == ENERGY_STORAGE;
            // Legacy crop harvester: transformer/energy storage/item producing.
            if (machine == MachineKind.CROP_HARVESTER)
                return this == TRANSFORMER || this == ENERGY_STORAGE || this == EJECTOR;
            if (machine == MachineKind.ITEM_BUFFER) return directional() && !fluid();
            if (machine == MachineKind.BLAST_FURNACE) return directional() && !fluid();
            if (machine == MachineKind.MATTER_GENERATOR) return directional() && !fluid();
            if (machine == MachineKind.CONDENSER) return directional() || this == TRANSFORMER;
            if (machine == MachineKind.ELECTROLYZER) return this == FLUID_PULLING;
            if (machine == MachineKind.TANK) return fluid();
            if (machine == MachineKind.FERMENTER || machine == MachineKind.LIQUID_HEAT_EXCHANGER)
                return directional();
            if (machine == MachineKind.INDUCTION_FURNACE) return this == EJECTOR || this == PULLING;
            // Legacy replicator: processing/transformer/storage/item consuming/item producing/
            // fluid consuming — no fluid ejector (no fluid producing property).
            if (machine == MachineKind.REPLICATOR)
                return this == OVERCLOCKER
                        || this == TRANSFORMER
                        || this == ENERGY_STORAGE
                        || this == EJECTOR
                        || this == PULLING
                        || this == FLUID_PULLING;
            return machine.upgradable()
                    && (!fluid()
                            || machine == MachineKind.CANNER
                            || this == FLUID_PULLING && machine == MachineKind.ORE_WASHING_PLANT);
        }
    }

    private final Kind kind;

    public UpgradeItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /**
     * Legacy Redstone.update reads {@code getBestNeighborSignal}, passes it through the machine's
     * upgrade modifiers ({@code 15 - external} for a redstone inverter) and every consumer then
     * checks {@code > 0}; this reproduces that chain exactly, returning the effective boolean
     * the machine's redstone gate consumes.
     */
    public static boolean invertedSignal(
            MachineKind machine, MachineInventory inventory, Level level, BlockPos pos) {
        int external = level.getBestNeighborSignal(pos);
        for (int slot = machine.upgradeStart(); slot < inventory.size(); slot++) {
            if (inventory.getResource(slot).getItem() instanceof UpgradeItem item
                    && item.kind() == Kind.REDSTONE_INVERTER) return 15 - external > 0;
        }
        return external > 0;
    }

    public static @Nullable Direction direction(ItemStack stack) {
        return stack.get(ModDataComponents.UPGRADE_DIRECTION);
    }

    public static Component directionName(ItemStack stack) {
        var direction = direction(stack);
        return Component.translatable(
                direction == null
                        ? "ic2.tooltip.upgrade.ejector.anyside"
                        : switch (direction) {
                            case DOWN -> "ic2.dir.bottom";
                            case UP -> "ic2.dir.top";
                            default -> "ic2.dir." + direction.getSerializedName();
                        });
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!kind.directional()) return InteractionResult.PASS;
        var player = context.getPlayer();
        if (player == null
                || !context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide()) {
            if (direction(stack) == context.getClickedFace())
                stack.remove(ModDataComponents.UPGRADE_DIRECTION);
            else stack.set(ModDataComponents.UPGRADE_DIRECTION, context.getClickedFace());
            player.sendOverlayMessage(
                    Component.translatable(
                            kind.pulling()
                                    ? "ic2.tooltip.upgrade.pulling"
                                    : "ic2.tooltip.upgrade.ejector",
                            directionName(stack)));
        }
        return InteractionResult.SUCCESS;
    }
}
