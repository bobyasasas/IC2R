package ic2.neoforge.crop;

import ic2.core.crop.CropMath;
import ic2.neoforge.component.CropSeed;
import ic2.neoforge.component.ModDataComponents;
import ic2.neoforge.registration.ModCrops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy TileEntityCrop growth kernel. The block state carries the crop type (one block per crop)
 * and its age; the tile carries the stats, stored resources, terrain quality and growth points. One
 * crop tick runs every 256 game ticks (legacy tickRate).
 */
public class CropBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public static final int TICK_RATE = 256;

    private int statGrowth;
    private int statGain;
    private int statResistance;
    private int storageNutrients;
    private int storageWater;
    private int storageWeedEx;
    private int terrainAirQuality = -1;
    private int terrainHumidity = -1;
    private int terrainNutrients = -1;
    private int growthPoints;
    private int scanLevel;

    public CropBlockEntity(BlockPos pos, BlockState state) {
        super(ModCrops.CROP_ENTITY.get(), pos, state);
    }

    /** Resolves the crop card from the block type, or null for an empty stick. */
    public ic2.neoforge.crop.CropCard card() {
        return ModCrops.cardFor(getBlockState().getBlock());
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % TICK_RATE == 0L) performTick(level.getGameTime());
    }

    /** Test/diagnostic hook: runs one crop tick at an explicit tick counter. */
    public void performTick(long ticker) {
        if (!(getLevel() instanceof ServerLevel level)) return;
        if (ticker % (TICK_RATE << 2) == 0L) updateTerrainHumidity(level);
        if ((ticker + TICK_RATE) % (TICK_RATE << 2) == 0L) updateTerrainNutrients(level);
        if ((ticker + TICK_RATE * 2L) % (TICK_RATE << 2) == 0L) updateTerrainAirQuality(level);

        var crop = card();
        if (crop != null) {
            crop.tick(this);
            if (crop.canGrow(this)) {
                performGrowthTick(level, crop);
                if (card() == null) return;
                if (growthPoints >= crop.getGrowthDuration(this)) {
                    growthPoints = 0;
                    setCurrentAge(getCurrentAge() + 1);
                }
            }
            if (storageNutrients > 0) storageNutrients--;
            if (storageWater > 0) storageWater--;
            if (crop.isWeed(this) && level.getRandom().nextInt(50) - statGrowth <= 2) {
                performWeedWork(level);
            }
        } else if (level.getRandom().nextInt(100) == 0 && storageWeedEx <= 0) {
            transformToWeed(level);
        } else if (storageWeedEx > 0 && level.getRandom().nextInt(10) == 0) {
            storageWeedEx--;
        }
        setChanged();
    }

    private void performGrowthTick(ServerLevel level, ic2.neoforge.crop.CropCard crop) {
        var result =
                CropMath.growth(
                        statGrowth,
                        statGain,
                        statResistance,
                        crop.getProperties().tier(),
                        crop.getWeightInfluences(
                                        this, terrainHumidity, terrainNutrients, terrainAirQuality)
                                * 5,
                        level.getRandom()::nextInt);
        if (result.reset()) {
            reset(level);
            return;
        }
        growthPoints += result.points();
    }

    /** Legacy performWeedWork: weeds spread over neighbours or wild grass on bare soil. */
    private void performWeedWork(ServerLevel level) {
        BlockPos dstPos =
                worldPosition.relative(
                        Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom()));
        if (level.getBlockEntity(dstPos) instanceof CropBlockEntity neighbour) {
            var neighbourCrop = neighbour.card();
            if (neighbourCrop == null
                    || !neighbourCrop.isWeed(neighbour)
                            && level.getRandom().nextInt(32) >= neighbour.statResistance
                            && !neighbour.hasWeedEx()) {
                int newGrowth = Math.max(statGrowth, neighbour.statGrowth);
                if (newGrowth < 31 && level.getRandom().nextBoolean()) newGrowth++;
                CropBlockEntity weedCrop = neighbour.transformToWeed(level);
                if (weedCrop != null) weedCrop.statGrowth = newGrowth;
            }
        } else if (level.isEmptyBlock(dstPos)) {
            BlockPos soilPos = dstPos.below();
            Block block = level.getBlockState(soilPos).getBlock();
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.FARMLAND) {
                level.setBlock(soilPos, Blocks.GRASS_BLOCK.defaultBlockState(), 7);
                level.setBlock(dstPos, Blocks.TALL_GRASS.defaultBlockState(), 7);
            }
        }
    }

    public boolean hasWeedEx() {
        if (storageWeedEx > 0) {
            storageWeedEx -= 5;
            return true;
        }
        return false;
    }

    /** Legacy transformCropBlock for the weed card: swaps this tile onto a weed crop block. */
    public CropBlockEntity transformToWeed(ServerLevel level) {
        var weed = ModCrops.WEED_CARD;
        return transformCropBlock(level, weed, 0);
    }

    /**
     * Legacy transformCropBlock: replaces the block (and thus the tile) with the crop's block at
     * the given age; callers seed the fresh tile with their stats afterwards.
     */
    public CropBlockEntity transformCropBlock(
            ServerLevel level, ic2.neoforge.crop.CropCard crop, int age) {
        CropBlock plantBlock = (CropBlock) crop.getCropBlock();
        BlockState newState = plantBlock.defaultBlockState();
        if (age > crop.getMaxAge()) age = crop.getMaxAge();
        if (age < 0) age = 0;
        if (plantBlock.ageProperty() != null)
            newState = newState.setValue(plantBlock.ageProperty(), age);
        level.setBlockAndUpdate(worldPosition, newState);
        return level.getBlockEntity(worldPosition) instanceof CropBlockEntity cropEntity
                ? cropEntity
                : null;
    }

    /**
     * Legacy setCrop through the single-arg transformCropBlock: swaps the crop keeping its age
     * (clamped to the new block) and refreshes the terrain; the stats reset with the fresh tile.
     */
    public void setCrop(ServerLevel level, ic2.neoforge.crop.CropCard crop) {
        CropBlock plantBlock = (CropBlock) crop.getCropBlock();
        BlockState newState = plantBlock.defaultBlockState();
        if (plantBlock.ageProperty() != null) {
            int age = Math.clamp(getCurrentAge(), 0, crop.getMaxAge());
            newState = newState.setValue(plantBlock.ageProperty(), age);
        }
        level.setBlockAndUpdate(worldPosition, newState);
        refreshTerrain(level);
    }

    /**
     * Legacy onEntityCollision: the card decides eligibility (and may poison), then the trample
     * roll may reset the crop and the soil below it.
     */
    public void onEntityCollision(net.minecraft.world.entity.Entity entity) {
        var crop = card();
        if (crop == null || !(getLevel() instanceof ServerLevel level)) return;
        if (crop.onEntityCollision(this, entity)
                && level.getRandom().nextInt(100) == 0
                && level.getRandom().nextInt(40) > statResistance) {
            reset(level);
            level.setBlock(worldPosition.below(), Blocks.DIRT.defaultBlockState(), 7);
        }
    }

    /** Legacy tryPlantIn: plants a card with stats; rejected on weeds or an already-grown crop. */
    public boolean tryPlantIn(
            ic2.neoforge.crop.CropCard crop,
            int size,
            int statGrowth,
            int statGain,
            int statResistance,
            int scan) {
        if (!(getLevel() instanceof ServerLevel level)) return false;
        if (crop == null || crop == ModCrops.WEED_CARD) return false;
        if (!crop.canGrow(this)) return false;
        CropBlockEntity planted = transformCropBlock(level, crop, size);
        if (planted == null) return false;
        planted.statGrowth = statGrowth;
        planted.statGain = statGain;
        planted.statResistance = statResistance;
        planted.scanLevel = scan;
        return true;
    }

    /** Legacy rightClick: harvest a mature crop, or plant a base seed produce on an empty stick. */
    public boolean rightClick(Player player, ItemStack held) {
        var crop = card();
        if (crop == null) {
            if (held != null && !held.isEmpty()) {
                var base = ModCrops.baseSeedFor(held.getItem());
                if (base != null) {
                    // Legacy consumeOrError with the registered size; zero consumes nothing.
                    if (base.size() > 0) held.shrink(base.size());
                    return tryPlantIn(
                            base.card(),
                            base.size(),
                            base.growth(),
                            base.gain(),
                            base.resistance(),
                            0);
                }
            }
            return false;
        }
        return crop.onRightClick(this, player);
    }

    public boolean rightClick(Player player) {
        return rightClick(player, ItemStack.EMPTY);
    }

    public boolean performManualHarvest() {
        List<ItemStack> drops = performHarvest();
        // Null means "not harvestable"; an empty list is a harvest whose gaussian rolled zero
        // drops — legacy still resets the plant in that case.
        if (drops == null) return false;
        if (getLevel() instanceof ServerLevel level) {
            for (ItemStack drop : drops) dropAsEntity(level, drop);
        }
        return true;
    }

    /** Legacy performHarvest: gaussian gain drops scaled by tier and gain stat. */
    public List<ItemStack> performHarvest() {
        var crop = card();
        if (crop == null || !crop.canBeHarvested(this)) return null;
        double chance = crop.dropGainChance() * Math.pow(1.03, statGain);
        ServerLevel level = (ServerLevel) getLevel();
        int dropCount =
                (int)
                        Math.max(
                                0L,
                                Math.round(
                                        level.getRandom().nextGaussian() * chance * 0.6827
                                                + chance));
        List<ItemStack> ret = new ArrayList<>();
        for (int i = 0; i < dropCount; i++) {
            for (ItemStack drop : crop.getGains(this)) {
                if (!drop.isEmpty() && level.getRandom().nextInt(100) <= statGain) {
                    drop.setCount(drop.getCount() + 1);
                }
                ret.add(drop);
            }
        }
        withCropAge(crop.getAgeAfterHarvest(this));
        return ret;
    }

    /** Legacy pick: breaking or left-clicking yields seeds by chance and resets the stick. */
    public boolean pick() {
        var crop = card();
        if (crop == null || !(getLevel() instanceof ServerLevel level)) return false;
        boolean bonus = crop.canBeHarvested(this);
        float firstChance = crop.dropSeedChance(this);
        firstChance = (float) (firstChance * Math.pow(1.1, statResistance));
        int dropCount = 0;
        if (bonus) {
            if (level.getRandom().nextFloat() <= (firstChance + 1.0F) * 0.8F) dropCount++;
            float chance = crop.dropSeedChance(this) + statGrowth / 100.0F;
            for (int i = 23; i < statGain; i++) chance *= 0.95F;
            if (level.getRandom().nextFloat() <= chance) dropCount++;
        } else if (level.getRandom().nextFloat() <= firstChance * 1.5F) {
            dropCount++;
        }
        for (int i = 0; i < dropCount; i++) dropAsEntity(level, crop.getSeedsItem(this));
        reset(level);
        return true;
    }

    public void reset(ServerLevel level) {
        resetData();
        level.setBlockAndUpdate(worldPosition, ModCrops.CROP_STICK.get().defaultBlockState());
    }

    public void resetData() {
        statGain = 0;
        statResistance = 0;
        statGrowth = 0;
        terrainAirQuality = -1;
        terrainHumidity = -1;
        terrainNutrients = -1;
        growthPoints = 0;
        scanLevel = 0;
        setChanged();
    }

    /** Legacy updateTerrainHumidity with the neutral (0) biome bonus of this slice. */
    public void updateTerrainHumidity(ServerLevel level) {
        int humidity = 0;
        BlockState below = level.getBlockState(worldPosition.below());
        if (below.getBlock() instanceof FarmlandBlock
                && below.getValue(FarmlandBlock.MOISTURE) >= 7) humidity += 2;
        if (storageWater >= 5) humidity += 2;
        humidity += (storageWater + 24) / 25;
        terrainHumidity = (byte) humidity;
    }

    /** Legacy updateTerrainNutrients with the neutral (0) biome bonus of this slice. */
    public void updateTerrainNutrients(ServerLevel level) {
        int nutrients = 0;
        for (int i = 1;
                i < 5 && level.getBlockState(worldPosition.below(i)).getBlock() == Blocks.DIRT;
                i++) nutrients++;
        nutrients += (storageNutrients + 19) / 20;
        terrainNutrients = (byte) nutrients;
    }

    /** Legacy updateTerrainAirQuality: altitude, 2x2 obstruction scan and open sky. */
    public void updateTerrainAirQuality(ServerLevel level) {
        int height = (int) Math.floor((worldPosition.getY() - 40) / 15.0);
        int altitude = Math.clamp(height, 0, 2);
        int fresh = 9;
        for (int x = worldPosition.getX() - 1; x < worldPosition.getX() + 1 && fresh > 0; x++) {
            for (int z = worldPosition.getZ() - 1; z < worldPosition.getZ() + 1 && fresh > 0; z++) {
                BlockPos cPos = new BlockPos(x, worldPosition.getY(), z);
                if (level.getBlockState(cPos).isCollisionShapeFullBlock(level, cPos)
                        || level.getBlockEntity(cPos) instanceof CropBlockEntity) fresh--;
            }
        }
        int value = CropMath.airQuality(altitude, fresh, level.canSeeSky(worldPosition.above()));
        terrainAirQuality = (byte) value;
    }

    /** Legacy setCrop convenience: refreshes all three terrain qualities from the world. */
    public void refreshTerrain(ServerLevel level) {
        updateTerrainHumidity(level);
        updateTerrainNutrients(level);
        updateTerrainAirQuality(level);
    }

    /** Legacy isBlockBelow: scans the root zone (down to rootsLength) without air gaps. */
    public boolean isBlockBelow(net.minecraft.world.level.block.Block reqBlock) {
        var crop = card();
        if (crop == null || !(getLevel() instanceof ServerLevel level)) return false;
        for (int i = 1; i < crop.getRootsLength(this); i++) {
            BlockPos below = worldPosition.below(i);
            BlockState state = level.getBlockState(below);
            if (state.isAir()) return false;
            if (state.is(reqBlock)) return true;
        }
        return false;
    }

    public int getLightLevel() {
        return getLevel() == null ? 0 : getLevel().getMaxLocalRawBrightness(worldPosition);
    }

    public int getCurrentAge() {
        if (getBlockState().getBlock() instanceof CropBlock cropBlock) {
            var property = cropBlock.ageProperty();
            return property == null ? 0 : getBlockState().getValue(property);
        }
        return 0;
    }

    public void setCurrentAge(int age) {
        withCropAge(age);
    }

    public void withCropAge(int age) {
        if (getBlockState().getBlock() instanceof CropBlock cropBlock
                && cropBlock.ageProperty() != null
                && getLevel() != null) {
            getLevel()
                    .setBlockAndUpdate(
                            worldPosition, getBlockState().setValue(cropBlock.ageProperty(), age));
        }
    }

    public ItemStack generateSeeds(
            ic2.neoforge.crop.CropCard crop, int growth, int gain, int resistance, int scan) {
        ItemStack stack = new ItemStack(ModCrops.CROP_SEED_BAG.get());
        stack.set(
                ModDataComponents.CROP_SEED.get(),
                new CropSeed(crop.getId(), growth, gain, resistance, scan));
        return stack;
    }

    private void dropAsEntity(ServerLevel level, ItemStack stack) {
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    stack);
        }
    }

    // Legacy stat/storage accessors used by cards and tests.
    public int getStatGrowth() {
        return statGrowth;
    }

    public void setStatGrowth(int value) {
        statGrowth = value;
    }

    public int getStatGain() {
        return statGain;
    }

    public void setStatGain(int value) {
        statGain = value;
    }

    public int getStatResistance() {
        return statResistance;
    }

    public void setStatResistance(int value) {
        statResistance = value;
    }

    public int getScanLevel() {
        return scanLevel;
    }

    public void setScanLevel(int value) {
        scanLevel = value;
    }

    public int getStorageWater() {
        return storageWater;
    }

    public void setStorageWater(int value) {
        storageWater = value;
    }

    public int getStorageNutrients() {
        return storageNutrients;
    }

    public void setStorageNutrients(int value) {
        storageNutrients = value;
    }

    public int getStorageWeedEx() {
        return storageWeedEx;
    }

    public void setStorageWeedEx(int value) {
        storageWeedEx = value;
    }

    public int getTerrainHumidity() {
        return terrainHumidity;
    }

    public int getTerrainNutrients() {
        return terrainNutrients;
    }

    public int getTerrainAirQuality() {
        return terrainAirQuality;
    }

    public int getGrowthPoints() {
        return growthPoints;
    }

    /** Legacy setGrowthPoints: card ticks (e.g. nether wart on soul sand) grant bonus points. */
    public void setGrowthPoints(int value) {
        growthPoints = value;
    }

    public Vec3 position() {
        return Vec3.atCenterOf(worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        statGrowth = input.getByteOr("growth", (byte) 0);
        statGain = input.getByteOr("gain", (byte) 0);
        statResistance = input.getByteOr("resistance", (byte) 0);
        storageNutrients = input.getShortOr("nutrients", (short) 0);
        storageWater = input.getShortOr("water", (short) 0);
        storageWeedEx = input.getShortOr("weed_ex", (short) 0);
        terrainAirQuality = input.getByteOr("air", (byte) -1);
        terrainHumidity = input.getByteOr("humidity", (byte) -1);
        terrainNutrients = input.getByteOr("soil", (byte) -1);
        growthPoints = input.getShortOr("growth_points", (short) 0);
        scanLevel = input.getByteOr("scan", (byte) 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("growth", (byte) statGrowth);
        output.putByte("gain", (byte) statGain);
        output.putByte("resistance", (byte) statResistance);
        output.putShort("nutrients", (short) storageNutrients);
        output.putShort("water", (short) storageWater);
        output.putShort("weed_ex", (short) storageWeedEx);
        output.putByte("air", (byte) terrainAirQuality);
        output.putByte("humidity", (byte) terrainHumidity);
        output.putByte("soil", (byte) terrainNutrients);
        output.putShort("growth_points", (short) growthPoints);
        output.putByte("scan", (byte) scanLevel);
    }
}
