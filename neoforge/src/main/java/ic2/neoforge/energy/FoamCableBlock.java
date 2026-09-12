package ic2.neoforge.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import ic2.core.energy.grid.CableSpec;
import ic2.neoforge.item.CutterItem;
import ic2.neoforge.registration.ModMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import ic2.neoforge.world.FoamBlock;

import java.util.List;
import java.util.Locale;

/**
 * The foam-covered counterpart of a cable: a separate block id that hides the wire under a soft
 * shell which random-ticks into a protective hardened layer. Breaking it only strips the foam
 * back off, revealing the original cable again.
 */
public class FoamCableBlock extends Block {
    static final Codec<CableSpec.Material> MATERIAL_CODEC =
            Codec.STRING.xmap(
                    name -> CableSpec.Material.valueOf(name.toUpperCase(Locale.ROOT)),
                    material -> material.name().toLowerCase(Locale.ROOT));
    public static final MapCodec<FoamCableBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            MATERIAL_CODEC
                                                    .fieldOf("material")
                                                    .forGetter(block -> block.material),
                                            Codec.INT
                                                    .fieldOf("insulation")
                                                    .forGetter(block -> block.insulation),
                                            propertiesCodec())
                                    .apply(instance, FoamCableBlock::new));
    public static final EnumProperty<Foam> FOAM = EnumProperty.create("foam", Foam.class);
    final CableSpec.Material material;
    final int insulation;
    private final CableSpec specification;

    public FoamCableBlock(CableSpec.Material material, int insulation, Properties properties) {
        super(properties);
        this.material = material;
        this.insulation = insulation;
        specification = material.insulated(insulation);
        registerDefaultState(stateDefinition.any().setValue(FOAM, Foam.SOFT));
    }

    public CableSpec.Material material() {
        return material;
    }

    public CableSpec specification() {
        return specification;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FOAM);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Dipped into wet construction foam it starts out soft, like the legacy placement path.
        return defaultBlockState();
    }

    /** The foam shell is a full cube no matter how the cable inside was routed. */
    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** Soft foam stays visually see-through to neighbour face culling, like the legacy occlusion. */
    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FOAM).isSoft() ? Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if (state.getBlock() != previous.getBlock() && level instanceof ServerLevel server)
            WorldEnergyNetworks.invalidate(server);
        scheduleHardening(state, level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        WorldEnergyNetworks.invalidate(level);
    }

    /**
     * 26.1.2 has no block-level chunk-load hook, so random ticks resurrect the hardening loop if
     * its scheduled tick was ever lost across a save, matching the legacy onLoad safety net.
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        scheduleHardening(state, level, pos);
    }

    private void scheduleHardening(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(FOAM).isSoft()
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tickFoamHardening(state, level, pos, random);
    }

    /** One hardening probe per scheduled tick, with the legacy foam cure curve by light level. */
    protected void tickFoamHardening(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FOAM).isSoft()) return;
        if (random.nextFloat() < FoamBlock.getHardenChance(level, pos, state, FoamBlock.FoamType.NORMAL)) {
            level.setBlockAndUpdate(pos, state.setValue(FOAM, Foam.HARD));
        } else {
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * Mining a foam cable never removes the wire: the shell pops off and the plain cable state
     * comes back, so the block survives with no drops of its own.
     */
    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            ItemStack toolStack,
            boolean willHarvest,
            FluidState fluid) {
        if (!level.isClientSide()) {
            var counterpart = ModMachines.cableCounterpart(this);
            if (counterpart != null) {
                level.setBlockAndUpdate(
                        pos, CableBlock.connectedState(counterpart.get(), level, pos));
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, toolStack, willHarvest, fluid);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var counterpart = ModMachines.cableCounterpart(this);
        if (counterpart != null) {
            return counterpart
                    .get()
                    .getDrops(counterpart.get().defaultBlockState(), builder);
        }
        return super.getDrops(state, builder);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        var counterpart = ModMachines.cableCounterpart(this);
        if (counterpart != null) {
            return counterpart
                    .get()
                    .getCloneItemStack(level, pos, counterpart.get().defaultBlockState(), includeData);
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // Hardened foam shields the insulation; soft foam lets the cutter through.
        if (state.getValue(FOAM).isHard()) return;
        CutterItem.strip(player, level, pos, state);
    }

    public enum Foam implements StringRepresentable {
        SOFT("soft"),
        HARD("hard");

        private final String name;

        Foam(String name) {
            this.name = name;
        }

        public boolean isSoft() {
            return this == SOFT;
        }

        public boolean isHard() {
            return this == HARD;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
