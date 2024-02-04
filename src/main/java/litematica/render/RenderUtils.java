package litematica.render;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

import malilib.config.value.HorizontalAlignment;
import malilib.config.value.VerticalAlignment;
import malilib.gui.util.GuiUtils;
import malilib.render.RenderContext;
import malilib.render.ShapeRenderUtils;
import malilib.render.buffer.VanillaWrappingVertexBuilder;
import malilib.render.buffer.VertexBuilder;
import malilib.render.inventory.InventoryRenderDefinition;
import malilib.render.inventory.InventoryRenderUtils;
import malilib.util.MathUtils;
import malilib.util.StringUtils;
import malilib.util.data.Color4f;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.RenderWrap;
import malilib.util.inventory.InventoryView;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.Vec3i;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.util.PositionUtils;
import litematica.util.value.BlockInfoAlignment;

public class RenderUtils
{
    /**
     * Returns true if the main rendering is on, and the schematic rendering is on,
     * taking into account the invert rendering hotkey.
     * This method does not check the schematic <i>block</i> rendering!
     * @return
     */
    public static boolean isSchematicCurrentlyRendered()
    {
        return Configs.Visuals.MAIN_RENDERING_TOGGLE.getBooleanValue() &&
               Configs.Visuals.SCHEMATIC_RENDERING.getBooleanValue() != Hotkeys.INVERT_SCHEMATIC_RENDER_STATE.getKeyBind().isKeyBindHeld();
    }

    /**
     * Returns true if the main, schematic and block rendering are all on,
     * taking into account the invert rendering hotkey.
     * @return
     */
    public static boolean areSchematicBlocksCurrentlyRendered()
    {
        return isSchematicCurrentlyRendered() &&
               Configs.Visuals.SCHEMATIC_BLOCKS_RENDERING.getBooleanValue();
    }

    public static int getMaxStringRenderLength(List<String> list)
    {
        int length = 0;

        for (String str : list)
        {
            length = Math.max(length, StringUtils.getStringWidth(str));
        }

        return length;
    }

    public static void renderBlockOutline(BlockPos pos,
                                          float expand,
                                          float lineWidth,
                                          Color4f color,
                                          Entity renderViewEntity,
                                          float partialTicks)
    {
        RenderWrap.lineWidth(lineWidth);

        AxisAlignedBB aabb = createAABB(pos.getX(), pos.getY(), pos.getZ(), expand, partialTicks, renderViewEntity);
        drawBoundingBoxEdges(aabb, color, color, color);
    }

    public static void drawBlockBoundingBoxOutlinesBatchedLines(long posLong,
                                                                Color4f color,
                                                                double expand,
                                                                VertexBuilder builder,
                                                                Entity renderViewEntity,
                                                                float partialTicks)
    {
        double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);
        double minX = malilib.util.position.PositionUtils.unpackX(posLong) - dx - expand;
        double minY = malilib.util.position.PositionUtils.unpackY(posLong) - dy - expand;
        double minZ = malilib.util.position.PositionUtils.unpackZ(posLong) - dz - expand;
        double maxX = malilib.util.position.PositionUtils.unpackX(posLong) - dx + expand + 1;
        double maxY = malilib.util.position.PositionUtils.unpackY(posLong) - dy + expand + 1;
        double maxZ = malilib.util.position.PositionUtils.unpackZ(posLong) - dz + expand + 1;

        ShapeRenderUtils.renderBoxEdgeLines(minX, minY, minZ, maxX, maxY, maxZ, color, builder);
    }

    public static void drawConnectingLineBatchedLines(long pos1,
                                                      long pos2,
                                                      boolean center,
                                                      Color4f color,
                                                      VertexBuilder builder,
                                                      Entity renderViewEntity,
                                                      float partialTicks)
    {
        double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);
        double x1 = malilib.util.position.PositionUtils.unpackX(pos1) - dx;
        double y1 = malilib.util.position.PositionUtils.unpackY(pos1) - dy;
        double z1 = malilib.util.position.PositionUtils.unpackZ(pos1) - dz;
        double x2 = malilib.util.position.PositionUtils.unpackX(pos2) - dx;
        double y2 = malilib.util.position.PositionUtils.unpackY(pos2) - dy;
        double z2 = malilib.util.position.PositionUtils.unpackZ(pos2) - dz;

        if (center)
        {
            x1 += 0.5;
            y1 += 0.5;
            z1 += 0.5;
            x2 += 0.5;
            y2 += 0.5;
            z2 += 0.5;
        }

        builder.posColor(x1, y1, z1, color);
        builder.posColor(x2, y2, z2, color);
    }

    public static void renderBlockOutlineOverlapping(BlockPos pos,
                                                     float expand,
                                                     float lineWidth,
                                                     Color4f color1,
                                                     Color4f color2,
                                                     Color4f color3,
                                                     Entity renderViewEntity,
                                                     float partialTicks)
    {
        final double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        final double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        final double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);

        final double minX = pos.getX() - dx - expand;
        final double minY = pos.getY() - dy - expand;
        final double minZ = pos.getZ() - dz - expand;
        final double maxX = pos.getX() - dx + expand + 1;
        final double maxY = pos.getY() - dy + expand + 1;
        final double maxZ = pos.getZ() - dz + expand + 1;

        RenderWrap.lineWidth(lineWidth);

        VertexBuilder builder = VanillaWrappingVertexBuilder.coloredLines();

        // Min corner
        builder.posColor(minX, minY, minZ, color1);
        builder.posColor(maxX, minY, minZ, color1);

        builder.posColor(minX, minY, minZ, color1);
        builder.posColor(minX, maxY, minZ, color1);

        builder.posColor(minX, minY, minZ, color1);
        builder.posColor(minX, minY, maxZ, color1);

        // Max corner
        builder.posColor(minX, maxY, maxZ, color2);
        builder.posColor(maxX, maxY, maxZ, color2);

        builder.posColor(maxX, minY, maxZ, color2);
        builder.posColor(maxX, maxY, maxZ, color2);

        builder.posColor(maxX, maxY, minZ, color2);
        builder.posColor(maxX, maxY, maxZ, color2);

        // The rest of the edges
        builder.posColor(minX, maxY, minZ, color3);
        builder.posColor(maxX, maxY, minZ, color3);

        builder.posColor(minX, minY, maxZ, color3);
        builder.posColor(maxX, minY, maxZ, color3);

        builder.posColor(maxX, minY, minZ, color3);
        builder.posColor(maxX, maxY, minZ, color3);

        builder.posColor(minX, minY, maxZ, color3);
        builder.posColor(minX, maxY, maxZ, color3);

        builder.posColor(maxX, minY, minZ, color3);
        builder.posColor(maxX, minY, maxZ, color3);

        builder.posColor(minX, maxY, minZ, color3);
        builder.posColor(minX, maxY, maxZ, color3);

        builder.draw();
    }

    public static void renderAreaOutline(IntBoundingBox box, float lineWidth,
                                         Color4f colorX, Color4f colorY, Color4f colorZ,
                                         Entity renderViewEntity, float partialTicks)
    {
        RenderWrap.lineWidth(lineWidth);

        AxisAlignedBB aabb = createAABB(box.minX, box.minY, box.minZ,
                                        box.maxX + 1, box.maxY + 1, box.maxZ + 1,
                                        0.0, partialTicks, renderViewEntity);
        drawBoundingBoxEdges(aabb, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(AxisAlignedBB box, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        drawBoundingBoxEdges(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ,
                                             Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        VertexBuilder builder = VanillaWrappingVertexBuilder.coloredLines();

        drawBoundingBoxLinesX(minX, minY, minZ, maxX, maxY, maxZ, colorX, builder);
        drawBoundingBoxLinesY(minX, minY, minZ, maxX, maxY, maxZ, colorY, builder);
        drawBoundingBoxLinesZ(minX, minY, minZ, maxX, maxY, maxZ, colorZ, builder);

        builder.draw();
    }

    private static void drawBoundingBoxLinesX(double minX, double minY, double minZ,
                                              double maxX, double maxY, double maxZ,
                                              Color4f color, VertexBuilder builder)
    {
        builder.posColor(minX, minY, minZ, color);
        builder.posColor(maxX, minY, minZ, color);

        builder.posColor(minX, maxY, minZ, color);
        builder.posColor(maxX, maxY, minZ, color);

        builder.posColor(minX, minY, maxZ, color);
        builder.posColor(maxX, minY, maxZ, color);

        builder.posColor(minX, maxY, maxZ, color);
        builder.posColor(maxX, maxY, maxZ, color);
    }

    private static void drawBoundingBoxLinesY(double minX, double minY, double minZ,
                                              double maxX, double maxY, double maxZ,
                                              Color4f color, VertexBuilder builder)
    {
        builder.posColor(minX, minY, minZ, color);
        builder.posColor(minX, maxY, minZ, color);

        builder.posColor(maxX, minY, minZ, color);
        builder.posColor(maxX, maxY, minZ, color);

        builder.posColor(minX, minY, maxZ, color);
        builder.posColor(minX, maxY, maxZ, color);

        builder.posColor(maxX, minY, maxZ, color);
        builder.posColor(maxX, maxY, maxZ, color);
    }

    private static void drawBoundingBoxLinesZ(double minX, double minY, double minZ,
                                              double maxX, double maxY, double maxZ,
                                              Color4f color, VertexBuilder builder)
    {
        builder.posColor(minX, minY, minZ, color);
        builder.posColor(minX, minY, maxZ, color);

        builder.posColor(maxX, minY, minZ, color);
        builder.posColor(maxX, minY, maxZ, color);

        builder.posColor(minX, maxY, minZ, color);
        builder.posColor(minX, maxY, maxZ, color);

        builder.posColor(maxX, maxY, minZ, color);
        builder.posColor(maxX, maxY, maxZ, color);
    }

    public static void renderAreaSides(BlockPos pos1, BlockPos pos2, Color4f color,
                                       Entity renderViewEntity, float partialTicks)
    {
        RenderWrap.enableBlend();
        RenderWrap.disableCull();

        VertexBuilder builder = VanillaWrappingVertexBuilder.coloredQuads();
        renderAreaSidesBatched(pos1, pos2, color, 0.002, renderViewEntity, partialTicks, builder);
        builder.draw();

        RenderWrap.enableCull();
        RenderWrap.disableBlend();
    }

    public static void renderAreaSides(IntBoundingBox box, Color4f color,
                                       Entity renderViewEntity, float partialTicks)
    {
        double expand = 0.002;
        double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);
        double minX = box.minX - dx - expand;
        double minY = box.minY - dy - expand;
        double minZ = box.minZ - dz - expand;
        double maxX = box.maxX - dx + expand + 1;
        double maxY = box.maxY - dy + expand + 1;
        double maxZ = box.maxZ - dz + expand + 1;

        VertexBuilder builder = VanillaWrappingVertexBuilder.coloredQuads();
        ShapeRenderUtils.renderBoxSideQuads(minX, minY, minZ, maxX, maxY, maxZ, color, builder);

        RenderWrap.disableCull();
        builder.draw();
        RenderWrap.enableCull();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color, double expand,
                                              Entity renderViewEntity, float partialTicks, VertexBuilder builder)
    {
        double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);
        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;

        ShapeRenderUtils.renderBoxSideQuads(minX, minY, minZ, maxX, maxY, maxZ, color, builder);
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void renderAreaSidesBatched(long pos1, long pos2, Color4f color, double expand,
                                              Entity renderViewEntity, float partialTicks, VertexBuilder builder)
    {
        double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);
        int x1 = malilib.util.position.PositionUtils.unpackX(pos1);
        int y1 = malilib.util.position.PositionUtils.unpackY(pos1);
        int z1 = malilib.util.position.PositionUtils.unpackZ(pos1);
        int x2 = malilib.util.position.PositionUtils.unpackX(pos2);
        int y2 = malilib.util.position.PositionUtils.unpackY(pos2);
        int z2 = malilib.util.position.PositionUtils.unpackZ(pos2);
        double minX = Math.min(x1, x2) - dx - expand;
        double minY = Math.min(y1, y2) - dy - expand;
        double minZ = Math.min(z1, z2) - dz - expand;
        double maxX = Math.max(x1, x2) + 1 - dx + expand;
        double maxY = Math.max(y1, y2) + 1 - dy + expand;
        double maxZ = Math.max(z1, z2) + 1 - dz + expand;

        ShapeRenderUtils.renderBoxSideQuads(minX, minY, minZ, maxX, maxY, maxZ, color, builder);
    }

    public static void renderAreaOutlineNoCorners(BlockPos pos1, BlockPos pos2, float lineWidth,
                                                  Color4f colorX, Color4f colorY, Color4f colorZ,
                                                  Entity renderViewEntity, float partialTicks)
    {
        final int xMin = Math.min(pos1.getX(), pos2.getX());
        final int yMin = Math.min(pos1.getY(), pos2.getY());
        final int zMin = Math.min(pos1.getZ(), pos2.getZ());
        final int xMax = Math.max(pos1.getX(), pos2.getX());
        final int yMax = Math.max(pos1.getY(), pos2.getY());
        final int zMax = Math.max(pos1.getZ(), pos2.getZ());

        final double expand = 0.001;
        final double dx = EntityWrap.lerpX(renderViewEntity, partialTicks);
        final double dy = EntityWrap.lerpY(renderViewEntity, partialTicks);
        final double dz = EntityWrap.lerpZ(renderViewEntity, partialTicks);

        final double dxMin = -dx - expand;
        final double dyMin = -dy - expand;
        final double dzMin = -dz - expand;
        final double dxMax = -dx + expand;
        final double dyMax = -dy + expand;
        final double dzMax = -dz + expand;

        final double minX = xMin + dxMin;
        final double minY = yMin + dyMin;
        final double minZ = zMin + dzMin;
        final double maxX = xMax + dxMax;
        final double maxY = yMax + dyMax;
        final double maxZ = zMax + dzMax;

        int start, end;

        VertexBuilder builder = VanillaWrappingVertexBuilder.coloredLines();

        // Edges along the X-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            builder.posColor(start + dxMin, minY, minZ, colorX);
            builder.posColor(end   + dxMax, minY, minZ, colorX);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            builder.posColor(start + dxMin, maxY + 1, minZ, colorX);
            builder.posColor(end   + dxMax, maxY + 1, minZ, colorX);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            builder.posColor(start + dxMin, minY, maxZ + 1, colorX);
            builder.posColor(end   + dxMax, minY, maxZ + 1, colorX);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            builder.posColor(start + dxMin, maxY + 1, maxZ + 1, colorX);
            builder.posColor(end   + dxMax, maxY + 1, maxZ + 1, colorX);
        }

        // Edges along the Y-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            builder.posColor(minX, start + dyMin, minZ, colorY);
            builder.posColor(minX, end   + dyMax, minZ, colorY);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            builder.posColor(maxX + 1, start + dyMin, minZ, colorY);
            builder.posColor(maxX + 1, end   + dyMax, minZ, colorY);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            builder.posColor(minX, start + dyMin, maxZ + 1, colorY);
            builder.posColor(minX, end   + dyMax, maxZ + 1, colorY);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            builder.posColor(maxX + 1, start + dyMin, maxZ + 1, colorY);
            builder.posColor(maxX + 1, end   + dyMax, maxZ + 1, colorY);
        }

        // Edges along the Z-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            builder.posColor(minX, minY, start + dzMin, colorZ);
            builder.posColor(minX, minY, end   + dzMax, colorZ);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            builder.posColor(maxX + 1, minY, start + dzMin, colorZ);
            builder.posColor(maxX + 1, minY, end   + dzMax, colorZ);
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            builder.posColor(minX, maxY + 1, start + dzMin, colorZ);
            builder.posColor(minX, maxY + 1, end   + dzMax, colorZ);
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            builder.posColor(maxX + 1, maxY + 1, start + dzMin, colorZ);
            builder.posColor(maxX + 1, maxY + 1, end   + dzMax, colorZ);
        }

        RenderWrap.lineWidth(lineWidth);
        builder.draw();
    }

    /**
     * Assumes a BufferBuilder in the GL_LINES mode has been initialized
     */
    public static void drawBlockModelOutlinesBatched(IBakedModel model, IBlockState state,
                                                     BlockPos pos, Color4f color, VertexBuilder builder)
    {
        long rand = MathUtils.getPositionRandom(pos);

        for (final EnumFacing side : PositionUtils.ALL_DIRECTIONS)
        {
            renderModelQuadOutlines(pos, color, model.getQuads(state, side, rand), builder);
        }

        renderModelQuadOutlines(pos, color, model.getQuads(state, null, rand), builder);
    }

    private static void renderModelQuadOutlines(BlockPos pos, Color4f color, List<BakedQuad> quads, VertexBuilder builder)
    {
        for (BakedQuad quad : quads)
        {
            renderQuadOutlinesBatched(pos, color, quad.getVertexData(), builder);
        }
    }

    private static void renderQuadOutlinesBatched(BlockPos pos, Color4f color, int[] vertexData, VertexBuilder builder)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        float[] fx = new float[4];
        float[] fy = new float[4];
        float[] fz = new float[4];

        for (int index = 0; index < 4; ++index)
        {
            fx[index] = x + Float.intBitsToFloat(vertexData[index * 7    ]);
            fy[index] = y + Float.intBitsToFloat(vertexData[index * 7 + 1]);
            fz[index] = z + Float.intBitsToFloat(vertexData[index * 7 + 2]);
        }

        builder.posColor(fx[0], fy[0], fz[0], color);
        builder.posColor(fx[1], fy[1], fz[1], color);

        builder.posColor(fx[1], fy[1], fz[1], color);
        builder.posColor(fx[2], fy[2], fz[2], color);

        builder.posColor(fx[2], fy[2], fz[2], color);
        builder.posColor(fx[3], fy[3], fz[3], color);

        builder.posColor(fx[3], fy[3], fz[3], color);
        builder.posColor(fx[0], fy[0], fz[0], color);
    }

    public static void drawBlockModelQuadOverlayBatched(IBakedModel model, IBlockState state, BlockPos pos,
                                                        Color4f color, double expand, VertexBuilder builder)
    {
        long rand = MathUtils.getPositionRandom(pos);

        for (final EnumFacing side : PositionUtils.ALL_DIRECTIONS)
        {
            renderModelQuadOverlayBatched(pos, color, model.getQuads(state, side, rand), builder);
        }

        renderModelQuadOverlayBatched(pos, color, model.getQuads(state, null, rand), builder);
    }

    public static void drawBlockModelQuadOverlayBatched(IBakedModel model, IBlockState state,
                                                        BlockPos pos, Direction side,
                                                        Color4f color, double expand, long rand, VertexBuilder builder)
    {
        renderModelQuadOverlayBatched(pos, color, model.getQuads(state, side.getVanillaDirection(), rand), builder);
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, Color4f color,
                                                      List<BakedQuad> quads, VertexBuilder builder)
    {
        for (BakedQuad quad : quads)
        {
            renderModelQuadOverlayBatched(pos, color, quad.getVertexData(), builder);
        }
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, Color4f color,
                                                      int[] vertexData, VertexBuilder builder)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        float fx, fy, fz;

        for (int index = 0; index < 4; ++index)
        {
            fx = x + Float.intBitsToFloat(vertexData[index * 7    ]);
            fy = y + Float.intBitsToFloat(vertexData[index * 7 + 1]);
            fz = z + Float.intBitsToFloat(vertexData[index * 7 + 2]);

            builder.posColor(fx, fy, fz, color);
        }
    }

    public static void drawBlockBoxEdgeBatchedLines(BlockPos pos, Direction.Axis axis,
                                                    int cornerIndex, Color4f color, VertexBuilder builder)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == Direction.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == Direction.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == Direction.Axis.Z ? 1 : 0);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        builder.posColor(minX, minY, minZ, color);
        builder.posColor(maxX, maxY, maxZ, color);
    }

    public static int renderInventoryOverlays(BlockInfoAlignment align, int offY,
                                              World worldSchematic, World worldClient, BlockPos pos)
    {
        int heightSch = renderInventoryOverlay(align, HorizontalAlignment.RIGHT, offY, worldSchematic, pos);
        int heightCli = renderInventoryOverlay(align, HorizontalAlignment.LEFT, offY, worldClient, pos);

        return Math.max(heightSch, heightCli);
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, HorizontalAlignment side,
                                             int offY, World world, BlockPos pos)
    {
        Pair<InventoryView, InventoryRenderDefinition> pair = InventoryRenderUtils.getInventoryViewFromBlock(world, pos);

        if (pair != null)
        {
            InventoryRenderDefinition renderer = pair.getRight();
            InventoryView inv = pair.getLeft();
            int gap = side == HorizontalAlignment.LEFT ? 4 : -4;
            int height = renderer.getRenderHeight(inv);
            final int xCenter = GuiUtils.getScaledWindowWidth() / 2 + gap;
            final int yCenter = GuiUtils.getScaledWindowHeight() / 2;
            int y = (align.getVerticalAlign() == VerticalAlignment.CENTER ? yCenter - height : 2) + offY;

            InventoryRenderUtils.renderInventoryPreview(inv, renderer, xCenter, y, 300, 0xFFFFFFFF, side,
                                                        VerticalAlignment.TOP, RenderContext.DUMMY);

            return height;
        }

        return 0;
    }

    /*
    private static void renderModelBrightnessColor(IBlockState state, IBakedModel model, float brightness, float r, float g, float b)
    {
        for (EnumFacing facing : EnumFacing.values())
        {
            renderModelBrightnessColorQuads(brightness, r, g, b, model.getQuads(state, facing, 0L));
        }

        renderModelBrightnessColorQuads(brightness, r, g, b, model.getQuads(state, null, 0L));
    }

    private static void renderModelBrightnessColorQuads(float brightness, float red, float green, float blue, List<BakedQuad> listQuads)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        int i = 0;

        for (int j = listQuads.size(); i < j; ++i)
        {
            BakedQuad quad = listQuads.get(i);
            bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            bufferbuilder.addVertexData(quad.getVertexData());

            if (quad.hasTintIndex())
            {
                bufferbuilder.putColorRGB_F4(red * brightness, green * brightness, blue * brightness);
            }
            else
            {
                bufferbuilder.putColorRGB_F4(brightness, brightness, brightness);
            }

            Vec3i direction = quad.getFace().getDirectionVec();
            bufferbuilder.putNormal(direction.getX(), direction.getY(), direction.getZ());

            tessellator.draw();
        }
    }
    */

    /*
    private static void renderModel(final IBlockState state, final IBakedModel model, final BlockPos pos, final int alpha)
    {
        //BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        //dispatcher.getBlockModelRenderer().renderModelBrightnessColor(model, 1f, 1f, 1f, 1f);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);

        for (final EnumFacing facing : EnumFacing.values())
        {
            renderQuads(state, pos, buffer, model.getQuads(state, facing, 0), alpha);
        }

        renderQuads(state, pos, buffer, model.getQuads(state, null, 0), alpha);
        tessellator.draw();
    }

    private static void renderQuads(final IBlockState state, final BlockPos pos, final BufferBuilder buffer, final List<BakedQuad> quads, final int alpha)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            final BakedQuad quad = quads.get(i);
            final int color = quad.getTintIndex() == -1 ? alpha | 0xffffff : getTint(state, pos, alpha, quad.getTintIndex());
            //LightUtil.renderQuadColor(buffer, quad, color);
            renderQuad(buffer, quad, color);
        }
    }

    public static void renderQuad(BufferBuilder buffer, BakedQuad quad, int auxColor)
    {
        buffer.addVertexData(quad.getVertexData());
        putQuadColor(buffer, quad, auxColor);
    }

    private static int getTint(final IBlockState state, final BlockPos pos, final int alpha, final int tintIndex)
    {
        Minecraft mc = Minecraft.getMinecraft();
        return alpha | mc.getBlockColors().colorMultiplier(state, null, pos, tintIndex);
    }

    private static void putQuadColor(BufferBuilder buffer, BakedQuad quad, int color)
    {
        float cb = color & 0xFF;
        float cg = (color >>> 8) & 0xFF;
        float cr = (color >>> 16) & 0xFF;
        float ca = (color >>> 24) & 0xFF;
        VertexFormat format = DefaultVertexFormats.ITEM; //quad.getFormat();
        int size = format.getIntegerSize();
        int offset = format.getColorOffset() / 4; // assumes that color is aligned

        for (int i = 0; i < 4; i++)
        {
            int vc = quad.getVertexData()[offset + size * i];
            float vcr = vc & 0xFF;
            float vcg = (vc >>> 8) & 0xFF;
            float vcb = (vc >>> 16) & 0xFF;
            float vca = (vc >>> 24) & 0xFF;
            int ncr = Math.min(0xFF, (int)(cr * vcr / 0xFF));
            int ncg = Math.min(0xFF, (int)(cg * vcg / 0xFF));
            int ncb = Math.min(0xFF, (int)(cb * vcb / 0xFF));
            int nca = Math.min(0xFF, (int)(ca * vca / 0xFF));

            IBufferBuilder bufferMixin = (IBufferBuilder) buffer;
            bufferMixin.putColorRGBA(bufferMixin.getColorIndexAccessor(4 - i), ncr, ncg, ncb, nca);
        }
    }
    */

    /*
    public static void renderQuadColorSlow(BufferBuilder wr, BakedQuad quad, int auxColor)
    {
        ItemConsumer cons;

        if(wr == Tessellator.getInstance().getBuffer())
        {
            cons = getItemConsumer();
        }
        else
        {
            cons = new ItemConsumer(new VertexBufferConsumer(wr));
        }

        float b = (float)  (auxColor & 0xFF) / 0xFF;
        float g = (float) ((auxColor >>>  8) & 0xFF) / 0xFF;
        float r = (float) ((auxColor >>> 16) & 0xFF) / 0xFF;
        float a = (float) ((auxColor >>> 24) & 0xFF) / 0xFF;

        cons.setAuxColor(r, g, b, a);
        quad.pipe(cons);
    }

    public static void renderQuadColor(BufferBuilder wr, BakedQuad quad, int auxColor)
    {
        if (quad.getFormat().equals(wr.getVertexFormat())) 
        {
            wr.addVertexData(quad.getVertexData());
            ForgeHooksClient.putQuadColor(wr, quad, auxColor);
        }
        else
        {
            renderQuadColorSlow(wr, quad, auxColor);
        }
    }
    */

    /**
     * Creates an AABB for rendering purposes, which is offset by the render view entity's movement and current partialTicks
     */
    public static AxisAlignedBB createEnclosingAABB(BlockPos pos1, BlockPos pos2, Entity renderViewEntity, float partialTicks)
    {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX()) + 1;
        int maxY = Math.max(pos1.getY(), pos2.getY()) + 1;
        int maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1;

        return createAABB(minX, minY, minZ, maxX, maxY, maxZ, 0, partialTicks, renderViewEntity);
    }

    /**
     * Creates an AABB for rendering purposes, which is offset by the render view entity's movement and current partialTicks
     */
    public static AxisAlignedBB createAABB(int x, int y, int z, double expand, float partialTicks, Entity renderViewEntity)
    {
        return createAABB(x, y, z, x + 1, y + 1, z + 1, expand, partialTicks, renderViewEntity);
    }

    /**
     * Creates an AABB for rendering purposes, which is offset by the render view entity's movement and current partialTicks
     */
    public static AxisAlignedBB createAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                           double expand, float partialTicks, Entity entity)
    {
        double dx = EntityWrap.lerpX(entity, partialTicks);
        double dy = EntityWrap.lerpY(entity, partialTicks);
        double dz = EntityWrap.lerpZ(entity, partialTicks);

        return new AxisAlignedBB(   minX - dx - expand, minY - dy - expand, minZ - dz - expand,
                                    maxX - dx + expand, maxY - dy + expand, maxZ - dz + expand);
    }
}
