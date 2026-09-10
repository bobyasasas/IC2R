package ic2.neoforge.world;

import ic2.neoforge.registration.ModFoam;
import ic2.neoforge.registration.ModMaterialBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Hardening construction foam; results are the foam wall and reinforced stone. */
public class FoamBlock extends Block {
    public static final EnumProperty<FoamType> TYPE = EnumProperty.create("type", FoamType.class);

    public FoamBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(TYPE, FoamType.NORMAL));
    }

    public static float getHardenChance(
            Level level, BlockPos pos, BlockState state, FoamType type) {
        int light = level.getMaxLocalRawBrightness(pos);
        if (state.getLightDampening() == 0) {
            for (var side : Direction.values()) {
                light = Math.max(light, level.getMaxLocalRawBrightness(pos.relative(side)));
            }
        }

        int avgTime = type.hardenTime * (16 - light);
        return 1.0F / (avgTime * 20);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void randomTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int tickSpeed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        if (tickSpeed <= 0) {
            throw new IllegalStateException(
                    "Foam was randomly ticked when world " + level + " is not ticking?");
        }

        FoamType type = state.getValue(TYPE);
        float chance = getHardenChance(level, pos, state, type) * 4096.0F / tickSpeed;
        if (random.nextFloat() < chance) {
            level.setBlockAndUpdate(pos, type.getResult());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        // Legacy StackUtil.consume reads the clicked hand only; the 26.1.2 block hook has no
        // hand context, so the main hand is preferred and the offhand is the fallback.
        for (var hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.SAND)) {
                continue;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!level.isClientSide()) {
                level.setBlockAndUpdate(pos, state.getValue(TYPE).getResult());
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    public enum FoamType implements StringRepresentable {
        NORMAL("normal", 300),
        REINFORCED("reinforced", 600);

        public final int hardenTime;
        private final String name;

        FoamType(String name, int hardenTime) {
            this.name = name;
            this.hardenTime = hardenTime;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public BlockState getResult() {
            return switch (this) {
                case NORMAL -> ModFoam.WALLS.get("light_gray").get().defaultBlockState();
                case REINFORCED ->
                        ModMaterialBlocks.MATERIALS
                                .get("reinforced_stone")
                                .get()
                                .defaultBlockState();
            };
        }
    }
}
