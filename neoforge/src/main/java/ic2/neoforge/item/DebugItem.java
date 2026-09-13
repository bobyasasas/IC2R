package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.crop.CropBlockEntity;
import ic2.neoforge.crop.CropCard;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.machine.MachineBlock;
import ic2.neoforge.machine.MachineBlockEntity;
import ic2.neoforge.machine.PersonalChestBlockEntity;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * Legacy ItemDebug: a creative diagnostic tool with six working modes (field dumps, tile data,
 * energy net dump, tile acceleration), fed by an infinite energy budget. Legacy cycled modes with
 * the mode-switch keybind; the port has no keybind layer, so sneaking while using cycles instead.
 * The legacy IDebuggable branch has no port-side implementer and the reactor read-out waits for
 * the reactor migration, so neither is carried over.
 */
public class DebugItem extends ElectricItem {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Legacy InfiniteElectricItemManager: everything reports infinite. The port forbids infinite
     * magnitudes, so the budget is a huge finite number and fresh stacks ship fully charged.
     */
    public static final ElectricItemSpec INFINITE_SPEC =
            new ElectricItemSpec(1.0e18, 1.0e18, 0, true);

    public DebugItem(Properties properties) {
        super(properties, INFINITE_SPEC);
    }

    /** Legacy ItemDebug.Mode, display strings included. */
    public enum Mode {
        InterfacesFields("Interfaces and Fields"),
        InterfacesFieldsRetrace("Interfaces and Fields (liquid/entity)"),
        TileData("Tile Data"),
        EnergyNet("Energy Net"),
        Accelerate("Accelerate"),
        AccelerateX100("Accelerate x100");

        public static final Mode[] modes = values();
        private final String name;

        Mode(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

    public static Mode mode(ItemStack stack) {
        int modeIdx = stack.getOrDefault(ModDataComponents.DEBUG_MODE.get(), 0);
        if (modeIdx < 0 || modeIdx >= Mode.modes.length) modeIdx = 0;
        return Mode.modes[modeIdx];
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.set(ModDataComponents.DEBUG_MODE.get(), mode.ordinal());
    }

    private static void cycleMode(ItemStack stack, Player player) {
        Mode mode = Mode.modes[(mode(stack).ordinal() + 1) % Mode.modes.length];
        setMode(stack, mode);
        player.sendSystemMessage(Component.literal("Debug Item Mode: " + mode.getName()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown() || level.isClientSide()) return InteractionResult.PASS;
        cycleMode(player.getItemInHand(hand), player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || level.isClientSide()) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            // Legacy IC2.keyboard.isModeSwitchKeyDown; the port cycles on sneak instead.
            cycleMode(stack, player);
            return InteractionResult.SUCCESS;
        }

        // Legacy IDebuggable branch dropped: no port block entity implements it.
        Output output = newOutput(player);
        switch (mode(stack)) {
            case InterfacesFields:
            case InterfacesFieldsRetrace:
                dumpInterfacesFields(level, pos, output);
                break;
            case TileData:
                dumpTileData(level, pos, player, output);
                break;
            case EnergyNet:
                if (!WorldEnergyNetworks.dumpDebugInfo(
                        (ServerLevel) level, pos, output::console, output::chat))
                    return InteractionResult.PASS;
                break;
            case Accelerate:
            case AccelerateX100:
                accelerate(level, pos, mode(stack) == Mode.Accelerate ? 1000 : 100000, output);
        }
        output.flush();
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (mode(stack) != Mode.InterfacesFieldsRetrace) return InteractionResult.PASS;
        Level level = player.level();
        if (level.isClientSide()) return InteractionResult.PASS;
        Output output = newOutput(player);
        output.both("[server] entity: %s", entityName(target));
        // Legacy special-cased dropped item entities here; interactLivingEntity never sees
        // them (they are no LivingEntity), so that branch is unreachable and dropped.
        output.flush();
        return InteractionResult.SUCCESS;
    }

    private static String entityName(Entity entity) {
        String name = entity.toString();
        return name.length() > 100 ? name.substring(0, 90) + "... (" + (name.length() - 90) + " more)" : name;
    }

    private static Output newOutput(Player player) {
        return new Output(
                blob -> LOGGER.info("Debug Item dump:\n{}", blob),
                line -> player.sendSystemMessage(Component.literal(line)));
    }

    /** Legacy accelerate: force-runs the target's block entity ticker or random ticks. */
    public static boolean accelerate(Level world, BlockPos pos, int count, Output output) {
        BlockState state = world.getBlockState(pos);
        BlockEntity be;
        if (state.hasBlockEntity() && (be = world.getBlockEntity(pos)) != null) {
            BlockEntityTicker<BlockEntity> ticker =
                    castTicker(state.getTicker(world, be.getType()));
            if (ticker == null) {
                return false;
            }

            output.chat("Running %d ticks on %s.", count, be);
            int changes = 0;
            int interruptCount = -1;

            for (int i = 0; i < count; i++) {
                if (be.isRemoved()) {
                    changes++;
                    state = world.getBlockState(pos);
                    if (!state.hasBlockEntity()
                            || (be = world.getBlockEntity(pos)) == null
                            || be.isRemoved()
                            || (ticker = castTicker(state.getTicker(world, be.getType())))
                                    == null) {
                        interruptCount = i;
                        break;
                    }
                }

                ticker.tick(world, pos, state, be);
            }            if (changes > 0) {
                if (interruptCount != -1) {
                    output.chat(
                            "The tile entity changed %d time(s), interrupted after %d updates.",
                            changes,
                            interruptCount);
                } else {
                    output.chat("The tile entity changed %d time(s).", changes);
                }
            }
        } else {
            if (!state.isRandomlyTicking()) {
                return false;
            }

            // Legacy said "on % (%s)" — a format typo that threw on this branch; fixed here.
            output.chat("Running up to %d ticks on %s (%s).", count, state.getBlock(), pos);

            for (int i = 0; i < count && world.getBlockState(pos) == state; i++) {
                state.randomTick((ServerLevel) world, pos, world.getRandom());
                if (world.getBlockState(pos) != state) {
                    output.chat("Ran %d ticks before a state change.", i);
                    break;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityTicker<BlockEntity> castTicker(BlockEntityTicker<?> ticker) {
        return (BlockEntityTicker<BlockEntity>) ticker;
    }

    /** Legacy TileData mode read-outs, translated to the port's machine and crop tiles. */
    public static void dumpTileData(Level level, BlockPos pos, Player player, Output output) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof MachineBlockEntity te) {
            BlockState state = te.getBlockState();
            if (state.hasProperty(MachineBlock.ACTIVE) && state.hasProperty(MachineBlock.FACING))
                output.chat(
                        "Block: Active=%b Facing=%s",
                        state.getValue(MachineBlock.ACTIVE),
                        state.getValue(MachineBlock.FACING));
            if (te.energyCapacity() > 0)
                output.chat("Energy: %.2f / %.2f", te.storedEnergy(), te.energyCapacity());
            if (te.fuelMaximum() > 0)
                output.chat("Fuel: %d / %d", te.fuelRemaining(), te.fuelMaximum());
        }

        // Legacy IReactor read-out waits for the reactor migration.

        if (tileEntity instanceof PersonalChestBlockEntity te) {
            // permits() claims an unowned safe, so ask it only after checking ownership.
            output.chat("PersonalBlock: CanAccess=%b", !te.owned() || te.permits(player));
        }

        if (tileEntity instanceof CropBlockEntity te) {
            CropCard crop = te.card();
            String id = crop != null ? crop.getId() : "none";
            output.chat(
                    "Crop: Crop=%s Size=%d Growth=%d Gain=%d Resistance=%d Nutrients=%d Water=%d"
                            + " GrowthPoints=%d%n Cross=%b",
                    id,
                    te.getCurrentAge(),
                    te.getStatGrowth(),
                    te.getStatGain(),
                    te.getStatResistance(),
                    te.getStorageNutrients(),
                    te.getStorageWater(),
                    te.getGrowthPoints(),
                    te.isCrossingBase());
        }
    }

    /** Legacy InterfacesFields mode: describe the block state, interfaces and reflected fields. */
    public static void dumpInterfacesFields(Level level, BlockPos pos, Output output) {
        String plat = "server";
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        BlockEntity te = level.getBlockEntity(pos);
        output.both(
                "[%s] block state: %s\nname: %s\ncls: %s\nbe: %s",
                plat,
                state,
                block.getDescriptionId(),
                block.getClass().getName(),
                te);
        if (te != null) {
            output.part("[%s] interfaces:", plat);
            Class<?> c = te.getClass();

            do {
                for (Class<?> i : c.getInterfaces()) {
                    output.part(' ').part(i.getName());
                }

                c = c.getSuperclass();
            } while (c != null);

            output.partToConsole();
        }

        output.console("block fields:");
        dumpObjectFields(block, output);
        if (te != null) {
            output.console("");
            output.console("tile entity fields:");
            dumpObjectFields(te, output);
        }
    }

    public static void dumpObjectFields(Object o, Output output) {
        List<Class<?>> classes = new ArrayList<>();
        Class<?> cls = o.getClass();

        do {
            classes.add(cls);
        } while ((cls = cls.getSuperclass()) != null);

        for (int clsIdx = classes.size() - 1; clsIdx >= 0; clsIdx--) {
            Class<?> fieldDeclaringClass = classes.get(clsIdx);
            Field[] fields = fieldDeclaringClass.getDeclaredFields();
            boolean printedHeader = false;

            for (Field field : fields) {
                Class<?> type = field.getType();
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers)
                        || fieldDeclaringClass != Block.class
                                && fieldDeclaringClass != BlockEntity.class
                                && fieldDeclaringClass != MachineBlockEntity.class
                                && (!Modifier.isFinal(modifiers)
                                        || !type.isPrimitive()
                                                && type != String.class
                                                && !Property.class.isAssignableFrom(type))) {
                    if (!printedHeader) {
                        output.console(fieldDeclaringClass.getName());
                        printedHeader = true;
                    }

                    Object value;
                    try {
                        field.setAccessible(true);
                        value = field.get(o);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        // Deep reflection may be denied in the module environment.
                        value = "<can't access>";
                    }

                    output.console("  %s type: %s", field.getName(), type.getName());
                    if (!isSelfDescribingClass(type)) {
                        output.part(
                                "    identity hash: %x hash: %x modifiers: %x",
                                System.identityHashCode(value),
                                value == null ? 0 : value.hashCode(),
                                modifiers);
                        if (value != null && value.getClass() != type) {
                            output.part(" class: %s", value.getClass().getName());
                        }

                        output.partToConsole();
                    }

                    if (value != null && field.getType().isArray()) {
                        List<Object> array = new ArrayList<>();

                        for (int i = 0; i < Array.getLength(value); i++) {
                            array.add(Array.get(value, i));
                        }

                        value = array;
                    }

                    if (value instanceof Iterable) {
                        output.part(
                                "    values (%s):",
                                value instanceof Collection<?> collection
                                        ? collection.size()
                                        : "?");
                        int i = 0;

                        for (Object o2 : (Iterable<?>) value) {
                            output.part("      [%d] ", i++);
                            dumpValueString(o2, field, "        ", output);
                        }
                    } else if (value instanceof Map) {
                        output.console("    values (%s):", ((Map<?, ?>) value).size());

                        for (Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                            output.part("      %s: ", entry.getKey());
                            dumpValueString(entry.getValue(), field, "        ", output);
                        }
                    } else {
                        output.part("    value: ");
                        dumpValueString(value, field, "      ", output);
                    }
                }
            }
        }
    }

    private static void dumpValueString(
            Object o, Field parentField, String prefix, Output output) {
        if (o == null) {
            output.part("<null>");
            output.partToConsole();
        } else {
            StringBuilder ret;
            if (o.getClass().isArray()) {
                ret = new StringBuilder();

                for (int i = 0; i < Array.getLength(o); i++) {
                    Object val = Array.get(o, i);
                    String valStr;
                    if (val == null) {
                        valStr = "<null>";
                    } else {
                        valStr = val.toString();
                        if (valStr.length() > 32) {
                            valStr =
                                    valStr.substring(0, 20)
                                            + "... ("
                                            + (valStr.length() - 20)
                                            + " more)";
                        }
                    }

                    ret.append(" [").append(i).append("] ").append(valStr);
                }
            } else {
                ret = new StringBuilder(o.toString());
            }

            if (ret.length() > 100) {
                ret =
                        new StringBuilder(
                                ret.substring(0, 90) + "... (" + (ret.length() - 90) + " more)");
            }

            output.part(ret.toString());
            output.partToConsole();
            if (!Modifier.isStatic(parentField.getModifiers())
                    && !parentField.isSynthetic()
                    && !o.getClass().isArray()
                    && !(o instanceof Iterable)
                    && !isSelfDescribingClass(o.getClass())) {
                if (o instanceof Level level) {
                    output.console("%s dim: %s", prefix, level.dimension().identifier());
                } else if (!(o instanceof StateDefinition)
                        && !(o instanceof BlockEntity)
                        && !(o instanceof ItemStack)
                        && !o.getClass().getName().startsWith("java.")) {
                    for (Class<?> fieldDeclaringClass = o.getClass();
                            fieldDeclaringClass != null
                                    && fieldDeclaringClass != Object.class;
                            fieldDeclaringClass = fieldDeclaringClass.getSuperclass()) {
                        for (Field field : fieldDeclaringClass.getDeclaredFields()) {
                            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                                Object val;
                                try {
                                    field.setAccessible(true);
                                    val = field.get(o);
                                } catch (ReflectiveOperationException | RuntimeException e) {
                                    val = "<can't access>";
                                }

                                String valStr;
                                if (val == o) {
                                    valStr = "<parent>";
                                } else {
                                    valStr = toStringLimited(val, 100);
                                }

                                output.console("%s%s: %s", prefix, field.getName(), valStr);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isSelfDescribingClass(Class<?> cls) {
        return cls.isPrimitive()
                || cls.isEnum()
                || cls == Class.class
                || cls == String.class
                || cls == BlockState.class
                || cls == Identifier.class
                || Tag.class.isAssignableFrom(cls)
                || Vec3i.class.isAssignableFrom(cls)
                || Vec3.class.isAssignableFrom(cls)
                || Block.class.isAssignableFrom(cls)
                || Item.class.isAssignableFrom(cls)
                || Fluid.class.isAssignableFrom(cls);
    }

    private static String toStringLimited(Object o, int limit) {
        if (o == null) {
            return "<null>";
        } else {
            int extra = 12;
            limit = Math.max(limit, 12);
            String ret = o.toString();
            if (ret.length() > limit) {
                int newLimit = limit - extra;
                return ret.substring(0, newLimit) + "... (" + (ret.length() - newLimit) + " more)";
            } else {
                return ret;
            }
        }
    }

    /**
     * Legacy ItemDebug.Output: buffered chat and console channels plus a partial-line scratch
     * area. The port pipes chat through system messages and the console through the server log.
     */
    public static final class Output {
        private final StringBuilder chatSb = new StringBuilder();
        private final StringBuilder consoleSb = new StringBuilder();
        private final StringBuilder partSb = new StringBuilder();
        private final Consumer<String> consoleSink;
        private final Consumer<String> chatSink;

        public Output(Consumer<String> consoleSink, Consumer<String> chatSink) {
            this.consoleSink = consoleSink;
            this.chatSink = chatSink;
        }

        public void chat(CharSequence line) {
            if (!this.chatSb.isEmpty()) {
                this.chatSb.append('\n');
            }

            this.chatSb.append(line);
        }

        public void chat(String format, Object... args) {
            this.chat(String.format(format, args));
        }

        public void console(CharSequence line) {
            if (!this.consoleSb.isEmpty()) {
                this.consoleSb.append('\n');
            }

            this.consoleSb.append(line);
        }

        public void console(String format, Object... args) {
            this.console(String.format(format, args));
        }

        public void both(CharSequence line) {
            this.chat(line);
            this.console(line);
        }

        public void both(String format, Object... args) {
            this.both(String.format(format, args));
        }

        public Output part(CharSequence line) {
            this.partSb.append(line);
            return this;
        }

        public Output part(char c) {
            this.partSb.append(c);
            return this;
        }

        public Output part(String format, Object... args) {
            return this.part(String.format(format, args));
        }

        public void partToChat() {
            this.chat(this.partSb);
            this.partSb.setLength(0);
        }

        public void partToConsole() {
            this.console(this.partSb);
            this.partSb.setLength(0);
        }

        public void partToBoth() {
            this.both(this.partSb);
            this.partSb.setLength(0);
        }

        public void flush() {
            if (!this.consoleSb.isEmpty()) {
                this.consoleSink.accept(this.consoleSb.toString());
            }

            for (String line : this.chatSb.toString().split("[\\r\\n]+")) {
                this.chatSink.accept(line);
            }

            this.chatSb.setLength(0);
            this.consoleSb.setLength(0);
        }
    }
}
