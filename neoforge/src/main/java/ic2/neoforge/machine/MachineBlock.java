package ic2.neoforge.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.energy.EnergyConfig;
import ic2.neoforge.energy.WorldEnergyNetworks;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MachineBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final MapCodec<MachineBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            MachineKind.CODEC
                                                    .fieldOf("kind")
                                                    .forGetter(block -> block.kind),
                                            propertiesCodec())
                                    .apply(instance, MachineBlock::new));
    private final MachineKind kind;

    /**
     * Legacy luminator panel: a full 16×16 plate one pixel thick, flush against the support
     * face — the side opposite the facing, which points away from the wall.
     */
    private static final Map<Direction, VoxelShape> LUMINATOR_PANEL = luminatorPanel();

    private static Map<Direction, VoxelShape> luminatorPanel() {
        var result = new EnumMap<Direction, VoxelShape>(Direction.class);
        double t = 1.0 / 16.0;
        result.put(Direction.NORTH, Shapes.box(0, 0, 1 - t, 1, 1, 1));
        result.put(Direction.SOUTH, Shapes.box(0, 0, 0, 1, 1, t));
        result.put(Direction.WEST, Shapes.box(1 - t, 0, 0, 1, 1, 1));
        result.put(Direction.EAST, Shapes.box(0, 0, 0, t, 1, 1));
        result.put(Direction.UP, Shapes.box(0, 0, 0, 1, t, 1));
        result.put(Direction.DOWN, Shapes.box(0, 1 - t, 0, 1, 1, 1));
        return result;
    }

    public MachineBlock(MachineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition.any().setValue(ACTIVE, false).setValue(FACING, Direction.NORTH));
    }

    public MachineKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        (kind.verticalFacing()
                                        ? context.getNearestLookingDirection()
                                        : context.getHorizontalDirection())
                                .getOpposite());
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == MachineKind.LUMINATOR
                ? LUMINATOR_PANEL.get(state.getValue(FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            net.minecraft.world.level.redstone.Orientation orientation,
            boolean moved) {
        if (kind == MachineKind.LUMINATOR
                && level instanceof ServerLevel
                && level.getBlockEntity(pos) instanceof LuminatorBlockEntity luminator)
            luminator.checkPlacement();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return ModMachines.createEntity(kind, position, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModMachines.entityType(kind)
                ? null
                : (world, pos, blockState, entity) -> {
                    if (entity instanceof MachineBlockEntity machine)
                        machine.serverTick((ServerLevel) world);
                };
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        var entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        for (var drop : drops) {
            // The loot table chooses the correct item and respects explosion survival.
            if (drop.is(asItem())
                    && kind.storage()
                    && entity instanceof EnergyStorageBlockEntity storage) {
                double retained =
                        storage.energy().stored() * EnergyConfig.STORAGE_DROP_RETENTION.get();
                if (retained > 0) drop.set(ModDataComponents.STORED_ENERGY, retained);
            }
        }
        return drops;
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof EnergyStorageBlockEntity storage) {
            double stored = stack.getOrDefault(ModDataComponents.STORED_ENERGY, 0.0);
            if (Double.isFinite(stored) && stored > 0) {
                storage.energy().restore(Math.min(stored, storage.energy().capacity()));
                storage.setChanged();
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.getCapability(Capabilities.Fluid.BLOCK, pos, hit.getDirection()) != null
                && ItemAccess.forPlayerInteraction(player, hand)
                                .oneByOne()
                                .getCapability(Capabilities.Fluid.ITEM)
                        != null) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (FluidUtil.interactWithFluidHandler(
                    player, hand, level, pos, hit.getDirection(), null))
                return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (machine instanceof TerraformerBlockEntity terraformer) {
                // Legacy terraformer has no GUI; right-click swaps the blueprint by hand.
                if (!player.isShiftKeyDown()) terraformer.useFromHand(player);
            } else if (machine instanceof LuminatorBlockEntity luminator) {
                // Legacy luminator has no GUI; right click discharges a held electric item
                // into the lamp or flips its redstone inversion.
                luminator.use(player, InteractionHand.MAIN_HAND);
            } else if (machine instanceof ManualKineticBlockEntity manual
                    && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                manual.turn(serverPlayer);
            } else player.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return kind.storage();
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof EnergyStorageBlockEntity storage
                ? storage.signal()
                : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return kind.storage()
                || kind == MachineKind.INDUCTION_FURNACE
                || kind == MachineKind.TANK
                || kind == MachineKind.LUMINATOR;
    }

    @Override
    protected int getAnalogOutputSignal(
            BlockState state, Level level, BlockPos pos, Direction side) {
        var machine = level.getBlockEntity(pos);
        if (machine instanceof EnergyStorageBlockEntity storage) return storage.comparator();
        if (machine instanceof TankBlockEntity tank) return tank.comparator();
        if (machine instanceof InductionFurnaceBlockEntity furnace) return furnace.comparator();
        if (machine instanceof LuminatorBlockEntity luminator) return luminator.comparator();
        return 0;
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if ((state.getBlock() != previous.getBlock()
                        || state.getValue(FACING) != previous.getValue(FACING))
                && level instanceof ServerLevel server) WorldEnergyNetworks.invalidate(server);
    }
}
