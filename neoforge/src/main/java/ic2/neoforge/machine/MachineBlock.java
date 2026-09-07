package ic2.neoforge.machine;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.neoforge.energy.WorldEnergyNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;

public final class MachineBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<net.minecraft.core.Direction> FACING =
            BlockStateProperties.HORIZONTAL_FACING;
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

    public MachineBlock(MachineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(ACTIVE, false)
                        .setValue(FACING, net.minecraft.core.Direction.NORTH));
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return switch (kind) {
            case GENERATOR -> new GeneratorBlockEntity(position, state);
            case ELECTRIC_FURNACE -> new ElectricFurnaceBlockEntity(position, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                        || type
                                != (kind == MachineKind.GENERATOR
                                        ? ic2.neoforge.registration.ModMachines.GENERATOR_ENTITY
                                                .get()
                                        : ic2.neoforge.registration.ModMachines
                                                .ELECTRIC_FURNACE_ENTITY
                                                .get())
                ? null
                : (world, pos, blockState, entity) -> {
                    if (entity instanceof PoweredBlockEntity machine)
                        machine.serverTick((ServerLevel) world);
                };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof PoweredBlockEntity machine) {
            player.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onPlace(
            BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if (state.getBlock() != previous.getBlock() && level instanceof ServerLevel server)
            WorldEnergyNetworks.invalidate(server);
    }
}
