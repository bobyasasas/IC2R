package ic2.neoforge.client;

import ic2.neoforge.machine.ChunkLoaderBlockEntity;
import ic2.neoforge.menu.MachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * Legacy GuiChunkLoader: the nine-by-nine chunk picker with terrain shades. Legacy rendered each
 * chunk as a 16x16 dynamic texture; the port averages each chunk's surface map colour into one
 * cell fill instead, trading texture management for a coarser preview.
 */
public final class ChunkLoaderScreen extends MachineScreen {
    private static final int CANVAS = ChunkLoaderBlockEntity.CANVAS;
    private static final int CELL = 16, CANVAS_X = 8, CANVAS_Y = 18;
    private static final int SELECTED = 0x3000FF00, UNSELECTED = 0x30FF0000;

    private final Long2IntOpenHashMap terrainShades = new Long2IntOpenHashMap();

    public ChunkLoaderScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected boolean showsEnergyBar() {
        return false;
    }

    @Override
    protected boolean showsProgress() {
        return false;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        drawEnergyBolt(graphics, x + 162, y + 18);
        for (int dz = 0; dz < CANVAS; dz++) {
            for (int dx = 0; dx < CANVAS; dx++) {
                int cellX = x + CANVAS_X + dx * CELL, cellY = y + CANVAS_Y + dz * CELL;
                var chunk = canvasChunk(dx, dz);
                graphics.fill(
                        cellX,
                        cellY,
                        cellX + CELL,
                        cellY + CELL,
                        terrainShades.computeIfAbsent(chunk.pack(), key -> shade(minecraft.level, chunk)));
                boolean selected = selected(dx, dz);
                graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, selected ? SELECTED : UNSELECTED);
            }
        }
        graphics.text(
                font,
                Component.literal(menu.familyValue(3) + " / " + menu.familyValue(4)),
                x + 160,
                y + 76,
                0xff404040,
                false);
    }

    private void drawEnergyBolt(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 12, y + 50, 0xff373737);
        int fill = (int) Math.clamp(48L * menu.energy() / Math.max(1, menu.capacity()), 0, 48);
        graphics.fill(x + 1, y + 49 - fill, x + 11, y + 49, 0xffe9ae23);
    }

    private ChunkPos canvasChunk(int dx, int dz) {
        var own = ChunkPos.containing(menu.machine().getBlockPos());
        return new ChunkPos(own.x() + dx - 4, own.z() + dz - 4);
    }

    private boolean selected(int dx, int dz) {
        int cell = dz * CANVAS + dx;
        return (menu.familyValue(cell / ChunkLoaderBlockEntity.WORD_BITS)
                        >>> cell % ChunkLoaderBlockEntity.WORD_BITS
                & 1)
                != 0;
    }

    /** Average surface map colour of the chunk; unloaded chunks stay black like legacy. */
    private int shade(Level level, ChunkPos chunk) {
        if (level == null || !level.hasChunk(chunk.x(), chunk.z())) return 0xff000000;
        LevelChunk loaded = level.getChunk(chunk.x(), chunk.z());
        int red = 0, green = 0, blue = 0, samples = 0;
        var pos = new BlockPos.MutableBlockPos();
        for (int cz = 0; cz < 16; cz += 2) {
            for (int cx = 0; cx < 16; cx += 2) {
                pos.set(
                        chunk.x() << 4 | cx,
                        loaded.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz),
                        chunk.z() << 4 | cz);
                var state = loaded.getBlockState(pos);
                if (state.isAir()) {
                    pos.move(0, -1, 0);
                    state = loaded.getBlockState(pos);
                }
                int color = state.getMapColor(level, pos).col;
                red += color >> 16 & 0xff;
                green += color >> 8 & 0xff;
                blue += color & 0xff;
                samples++;
            }
        }
        return 0xff000000 | red / samples << 16 | green / samples << 8 | blue / samples;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int dx = (int) (event.x() - leftPos - CANVAS_X) / CELL;
        int dz = (int) (event.y() - topPos - CANVAS_Y) / CELL;
        if (event.button() == 0 && dx >= 0 && dx < CANVAS && dz >= 0 && dz < CANVAS) {
            send(dz * CANVAS + dx);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int dx = (mouseX - leftPos - CANVAS_X) / CELL;
        int dz = (mouseY - topPos - CANVAS_Y) / CELL;
        if (dx >= 0 && dx < CANVAS && dz >= 0 && dz < CANVAS) {
            var chunk = canvasChunk(dx, dz);
            graphics.setComponentTooltipForNextFrame(
                    font,
                    java.util.List.of(
                            Component.literal("Chunk [" + chunk.x() + ", " + chunk.z() + "]"),
                            Component.translatable(
                                    selected(dx, dz)
                                            ? "ic2.chunkloader.selected"
                                            : "ic2.chunkloader.unselected")),
                    mouseX,
                    mouseY,
                    ItemStack.EMPTY);
        }
    }

    private void send(int id) {
        if (minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    public void removed() {
        super.removed();
        terrainShades.clear();
    }
}
