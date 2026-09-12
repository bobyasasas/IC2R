package ic2.neoforge.item;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.machine.MachineKind;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import org.jspecify.annotations.Nullable;

public final class UpgradeItem extends Item {
    public enum Kind {
        OVERCLOCKER,
        TRANSFORMER,
        ENERGY_STORAGE,
        EJECTOR,
        PULLING,
        FLUID_EJECTOR,
        FLUID_PULLING;

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
