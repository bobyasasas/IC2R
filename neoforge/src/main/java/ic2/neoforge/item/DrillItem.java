package ic2.neoforge.item;

import ic2.core.energy.ElectricItemSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Electric drill mining pickaxe- and shovel-mineable blocks while charged. Speed collapses to the
 * unpowered rate once the stored EU no longer covers one operation, like legacy.
 */
public class DrillItem extends ElectricItem {
    private final float miningSpeed;
    private final double operationEnergyCost;
    private final double harvestEnergyCost;
    private final int minerEnergyPerTick;
    private final int minerDuration;
    private final int fortuneLevel;

    public DrillItem(
            Properties properties,
            ElectricItemSpec specification,
            TagKey<Block> incorrectForDrops,
            float miningSpeed,
            double operationEnergyCost,
            double harvestEnergyCost,
            int minerEnergyPerTick,
            int minerDuration,
            int fortuneLevel) {
        super(
                properties.component(DataComponents.TOOL, tool(incorrectForDrops, miningSpeed)),
                specification);
        this.miningSpeed = miningSpeed;
        this.operationEnergyCost = operationEnergyCost;
        this.harvestEnergyCost = harvestEnergyCost;
        this.minerEnergyPerTick = minerEnergyPerTick;
        this.minerDuration = minerDuration;
        this.fortuneLevel = fortuneLevel;
    }

    /** Drill tool rules from a tool material, extended to both pickaxe and shovel blocks. */
    private static Tool tool(TagKey<Block> incorrectForDrops, float speed) {
        HolderGetter<Block> blocks =
                BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK);
        return new Tool(
                List.of(
                        Tool.Rule.deniesDrops(blocks.getOrThrow(incorrectForDrops)),
                        Tool.Rule.minesAndDrops(
                                blocks.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), speed),
                        Tool.Rule.minesAndDrops(
                                blocks.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), speed)),
                1.0F,
                0,
                true);
    }

    public boolean canUse(ItemStack stack) {
        return ElectricItemEnergy.charge(stack) >= operationEnergyCost;
    }

    /** Legacy drops require an effective block: pickaxe- or shovel-mineable, not energy-bound. */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return super.isCorrectToolForDrops(stack, state)
                && (state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                        || state.is(BlockTags.MINEABLE_WITH_SHOVEL));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return speed == 1.0F || !canUse(stack) ? 1.0F : this.miningSpeed;
    }

    @Override
    public boolean mineBlock(
            ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F && canUse(stack)) {
            ElectricItemEnergy.use(stack, operationEnergyCost, owner);
        }
        return true;
    }

    /** EU consumed by the miner for one harvest through this drill. */
    public double harvestEnergyCost() {
        return harvestEnergyCost;
    }

    /** EU per tick the miner spends drilling one block with this drill. */
    public int minerEnergyPerTick() {
        return minerEnergyPerTick;
    }

    /** Ticks the miner spends drilling one block with this drill. */
    public int minerDuration() {
        return minerDuration;
    }

    /** Fortune level the miner applies to drops, matching the legacy iridium bonus. */
    public int fortuneLevel() {
        return fortuneLevel;
    }
}
