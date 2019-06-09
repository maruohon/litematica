package fi.dy.masa.litematica.render;

import java.util.List;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import fi.dy.masa.litematica.util.BlockInfoAlignment;
import fi.dy.masa.litematica.util.InventoryUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryProperties;
import fi.dy.masa.malilib.render.InventoryOverlay.InventoryRenderType;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class RenderUtils
{
    private static final Random RAND = new Random();

    public static int getMaxStringRenderLength(List<String> list)
    {
        int length = 0;

        for (String str : list)
        {
            length = Math.max(length, StringUtils.getStringWidth(str));
        }

        return length;
    }

    public static void renderBlockOutline(BlockPos pos, float expand, float lineWidth,
            Color4f color, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.lineWidth(lineWidth);

        AxisAlignedBB aabb = createAABB(pos.getX(), pos.getY(), pos.getZ(), expand, partialTicks, renderViewEntity);
        WorldRenderer.drawSelectionBoundingBox(aabb, color.r, color.g, color.b, color.a);
    }

    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, Color4f color, double expand, BufferBuilder buffer,
            Entity renderViewEntity, float partialTicks)
    {
        double dx = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        double dy = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        double dz = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
        double minX = pos.getX() - dx - expand;
        double minY = pos.getY() - dy - expand;
        double minZ = pos.getZ() - dz - expand;
        double maxX = pos.getX() - dx + expand + 1;
        double maxY = pos.getY() - dy + expand + 1;
        double maxZ = pos.getZ() - dz + expand + 1;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void drawConnectingLineBatchedLines(BlockPos pos1, BlockPos pos2, boolean center, Color4f color, BufferBuilder buffer,
            Entity renderViewEntity, float partialTicks)
    {
        double dx = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        double dy = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        double dz = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
        double x1 = pos1.getX() - dx;
        double y1 = pos1.getY() - dy;
        double z1 = pos1.getZ() - dz;
        double x2 = pos2.getX() - dx;
        double y2 = pos2.getY() - dy;
        double z2 = pos2.getZ() - dz;

        if (center)
        {
            x1 += 0.5;
            y1 += 0.5;
            z1 += 0.5;
            x2 += 0.5;
            y2 += 0.5;
            z2 += 0.5;
        }

        buffer.pos(x1, y1, z1).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(x2, y2, z2).color(color.r, color.g, color.b, color.a).endVertex();
    }

    public static void renderBlockOutlineOverlapping(BlockPos pos, float expand, float lineWidth,
            Color4f color1, Color4f color2, Color4f color3, Entity renderViewEntity, float partialTicks)
    {
        final double dx = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        final double dy = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        final double dz = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;

        final double minX = pos.getX() - dx - expand;
        final double minY = pos.getY() - dy - expand;
        final double minZ = pos.getZ() - dz - expand;
        final double maxX = pos.getX() - dx + expand + 1;
        final double maxY = pos.getY() - dy + expand + 1;
        final double maxZ = pos.getZ() - dz + expand + 1;

        GlStateManager.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Min corner
        buffer.pos(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();
        buffer.pos(maxX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();

        buffer.pos(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();
        buffer.pos(minX, maxY, minZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();

        buffer.pos(minX, minY, minZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();
        buffer.pos(minX, minY, maxZ).color(color1.r, color1.g, color1.b, color1.a).endVertex();

        // Max corner
        buffer.pos(minX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();

        buffer.pos(maxX, minY, maxZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();

        buffer.pos(maxX, maxY, minZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color2.r, color2.g, color2.b, color2.a).endVertex();

        // The rest of the edges
        buffer.pos(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        buffer.pos(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        buffer.pos(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        buffer.pos(minX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        buffer.pos(maxX, minY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        buffer.pos(minX, maxY, minZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color3.r, color3.g, color3.b, color3.a).endVertex();

        tessellator.draw();
    }

    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
            Color4f colorX, Color4f colorY, Color4f colorZ, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.lineWidth(lineWidth);

        AxisAlignedBB aabb = createEnclosingAABB(pos1, pos2, renderViewEntity, partialTicks);
        drawBoundingBoxEdges(aabb, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(AxisAlignedBB box, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        drawBoundingBoxEdges(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorX, colorY, colorZ);
    }

    private static void drawBoundingBoxEdges(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f colorX, Color4f colorY, Color4f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        drawBoundingBoxLinesX(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorX);
        drawBoundingBoxLinesY(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorY);
        drawBoundingBoxLinesZ(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorZ);

        tessellator.draw();
    }

    private static void drawBoundingBoxLinesX(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color4f color)
    {
        buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
    }

    public static void renderAreaSides(BlockPos pos1, BlockPos pos2, Color4f color, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        renderAreaSidesBatched(pos1, pos2, color, 0.002, renderViewEntity, partialTicks, buffer);

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void renderAreaSidesBatched(BlockPos pos1, BlockPos pos2, Color4f color, double expand,
            Entity renderViewEntity, float partialTicks, BufferBuilder buffer)
    {
        double dx = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        double dy = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        double dz = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
        double minX = Math.min(pos1.getX(), pos2.getX()) - dx - expand;
        double minY = Math.min(pos1.getY(), pos2.getY()) - dy - expand;
        double minZ = Math.min(pos1.getZ(), pos2.getZ()) - dz - expand;
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1 - dx + expand;
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1 - dy + expand;
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1 - dz + expand;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    public static void renderAreaOutlineNoCorners(BlockPos pos1, BlockPos pos2,
            float lineWidth, Color4f colorX, Color4f colorY, Color4f colorZ, Entity renderViewEntity, float partialTicks)
    {
        final int xMin = Math.min(pos1.getX(), pos2.getX());
        final int yMin = Math.min(pos1.getY(), pos2.getY());
        final int zMin = Math.min(pos1.getZ(), pos2.getZ());
        final int xMax = Math.max(pos1.getX(), pos2.getX());
        final int yMax = Math.max(pos1.getY(), pos2.getY());
        final int zMax = Math.max(pos1.getZ(), pos2.getZ());

        final double expand = 0.001;
        final double dx = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        final double dy = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        final double dz = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;

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

        GlStateManager.lineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Edges along the X-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.pos(start + dxMin, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
            buffer.pos(end   + dxMax, minY, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.pos(start + dxMin, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
            buffer.pos(end   + dxMax, maxY + 1, minZ).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.pos(start + dxMin, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
            buffer.pos(end   + dxMax, minY, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMin + 1 : xMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? xMax : xMax + 1;

        if (end > start)
        {
            buffer.pos(start + dxMin, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
            buffer.pos(end   + dxMax, maxY + 1, maxZ + 1).color(colorX.r, colorX.g, colorX.b, colorX.a).endVertex();
        }

        // Edges along the Y-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.pos(minX, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
            buffer.pos(minX, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.pos(maxX + 1, start + dyMin, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
            buffer.pos(maxX + 1, end   + dyMax, minZ).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.pos(minX, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
            buffer.pos(minX, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? yMin + 1 : yMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? yMax : yMax + 1;

        if (end > start)
        {
            buffer.pos(maxX + 1, start + dyMin, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
            buffer.pos(maxX + 1, end   + dyMax, maxZ + 1).color(colorY.r, colorY.g, colorY.b, colorY.a).endVertex();
        }

        // Edges along the Z-axis
        start = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.pos(minX, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
            buffer.pos(minX, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMin && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMin && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.pos(maxX + 1, minY, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
            buffer.pos(maxX + 1, minY, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
        }

        start = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMin && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMin && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.pos(minX, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
            buffer.pos(minX, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
        }

        start = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMin) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMin) ? zMin + 1 : zMin;
        end   = (pos1.getX() == xMax && pos1.getY() == yMax && pos1.getZ() == zMax) || (pos2.getX() == xMax && pos2.getY() == yMax && pos2.getZ() == zMax) ? zMax : zMax + 1;

        if (end > start)
        {
            buffer.pos(maxX + 1, maxY + 1, start + dzMin).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
            buffer.pos(maxX + 1, maxY + 1, end   + dzMax).color(colorZ.r, colorZ.g, colorZ.b, colorZ.a).endVertex();
        }

        tessellator.draw();
    }

    /**
     * Assumes a BufferBuilder in the GL_LINES mode has been initialized
     */
    public static void drawBlockModelOutlinesBatched(IBakedModel model, IBlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final EnumFacing side : PositionUtils.FACING_ALL)
        {
            renderModelQuadOutlines(pos, buffer, color, model.getQuads(state, side, RAND));
        }

        renderModelQuadOutlines(pos, buffer, color, model.getQuads(state, null, RAND));
    }

    private static void renderModelQuadOutlines(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            renderQuadOutlinesBatched(pos, buffer, color, quads.get(i).getVertexData());
        }
    }

    private static void renderQuadOutlinesBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        float fx[] = new float[4];
        float fy[] = new float[4];
        float fz[] = new float[4];

        for (int index = 0; index < 4; ++index)
        {
            fx[index] = x + Float.intBitsToFloat(vertexData[index * 7 + 0]);
            fy[index] = y + Float.intBitsToFloat(vertexData[index * 7 + 1]);
            fz[index] = z + Float.intBitsToFloat(vertexData[index * 7 + 2]);
        }

        buffer.pos(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(fx[1], fy[1], fz[1]).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(fx[2], fy[2], fz[2]).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).endVertex();

        buffer.pos(fx[3], fy[3], fz[3]).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(fx[0], fy[0], fz[0]).color(color.r, color.g, color.b, color.a).endVertex();
    }

    public static void drawBlockModelQuadOverlayBatched(IBakedModel model, IBlockState state, BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        for (final EnumFacing side : PositionUtils.FACING_ALL)
        {
            renderModelQuadOverlayBatched(pos, buffer, color, model.getQuads(state, side, RAND));
        }

        renderModelQuadOverlayBatched(pos, buffer, color, model.getQuads(state, null, RAND));
    }

    public static void drawBlockModelQuadOverlayBatched(IBakedModel model, IBlockState state, BlockPos pos, EnumFacing side, Color4f color, double expand, BufferBuilder buffer)
    {
        renderModelQuadOverlayBatched(pos, buffer, color, model.getQuads(state, side, RAND));
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, List<BakedQuad> quads)
    {
        final int size = quads.size();

        for (int i = 0; i < size; i++)
        {
            renderModelQuadOverlayBatched(pos, buffer, color, quads.get(i).getVertexData());
        }
    }

    private static void renderModelQuadOverlayBatched(BlockPos pos, BufferBuilder buffer, Color4f color, int[] vertexData)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        float fx, fy, fz;

        for (int index = 0; index < 4; ++index)
        {
            fx = x + Float.intBitsToFloat(vertexData[index * 7 + 0]);
            fy = y + Float.intBitsToFloat(vertexData[index * 7 + 1]);
            fz = z + Float.intBitsToFloat(vertexData[index * 7 + 2]);

            buffer.pos(fx, fy, fz).color(color.r, color.g, color.b, color.a).endVertex();
        }
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockBoxSideBatchedQuads(BlockPos pos, EnumFacing side, Color4f color, double expand, BufferBuilder buffer)
    {
        double minX = pos.getX() - expand;
        double minY = pos.getY() - expand;
        double minZ = pos.getZ() - expand;
        double maxX = pos.getX() + expand + 1;
        double maxY = pos.getY() + expand + 1;
        double maxZ = pos.getZ() + expand + 1;

        switch (side)
        {
            case DOWN:
                buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;

            case UP:
                buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;

            case NORTH:
                buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;

            case SOUTH:
                buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;

            case WEST:
                buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;

            case EAST:
                buffer.pos(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
                buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
                break;
        }
    }

    public static void drawBlockBoxEdgeBatchedLines(BlockPos pos, EnumFacing.Axis axis, int cornerIndex, Color4f color, BufferBuilder buffer)
    {
        Vec3i offset = PositionUtils.getEdgeNeighborOffsets(axis, cornerIndex)[cornerIndex];

        double minX = pos.getX() + offset.getX();
        double minY = pos.getY() + offset.getY();
        double minZ = pos.getZ() + offset.getZ();
        double maxX = pos.getX() + offset.getX() + (axis == EnumFacing.Axis.X ? 1 : 0);
        double maxY = pos.getY() + offset.getY() + (axis == EnumFacing.Axis.Y ? 1 : 0);
        double maxZ = pos.getZ() + offset.getZ() + (axis == EnumFacing.Axis.Z ? 1 : 0);

        //System.out.printf("pos: %s, axis: %s, ind: %d\n", pos, axis, cornerIndex);
        buffer.pos(minX, minY, minZ).color(color.r, color.g, color.b, color.a).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).endVertex();
    }

    public static int renderInventoryOverlays(BlockInfoAlignment align, int offY, World worldSchematic, World worldClient, BlockPos pos, Minecraft mc)
    {
        int heightSch = renderInventoryOverlay(align, LeftRight.LEFT, offY, worldSchematic, pos, mc);
        int heightCli = renderInventoryOverlay(align, LeftRight.RIGHT, offY, worldClient, pos, mc);

        return Math.max(heightSch, heightCli);
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
            World world, BlockPos pos, Minecraft mc)
    {
        IInventory inv = InventoryUtils.getInventory(world, pos);

        if (inv != null)
        {
            final InventoryRenderType type = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryType(inv);
            final InventoryProperties props = fi.dy.masa.malilib.render.InventoryOverlay.getInventoryPropsTemp(type, inv.getSizeInventory());

            return renderInventoryOverlay(align, side, offY, inv, type, props, mc);
        }

        return 0;
    }

    public static int renderInventoryOverlay(BlockInfoAlignment align, LeftRight side, int offY,
            IInventory inv, InventoryRenderType type, InventoryProperties props, Minecraft mc)
    {
        int xInv = 0;
        int yInv = 0;

        switch (align)
        {
            case CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = GuiUtils.getScaledWindowHeight() / 2 - props.height - offY;
                break;
            case TOP_CENTER:
                xInv = GuiUtils.getScaledWindowWidth() / 2 - (props.width / 2);
                yInv = offY;
                break;
        }

        if      (side == LeftRight.LEFT)  { xInv -= (props.width / 2 + 4); }
        else if (side == LeftRight.RIGHT) { xInv += (props.width / 2 + 4); }

        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryBackground(type, xInv, yInv, props.slotsPerRow, props.totalSlots, mc);
        fi.dy.masa.malilib.render.InventoryOverlay.renderInventoryStacks(type, inv, xInv + props.slotOffsetX, yInv + props.slotOffsetY, props.slotsPerRow, 0, -1, mc);

        return props.height;
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
    public static AxisAlignedBB createAABB(int x, int y, int z, double expand, double partialTicks, Entity renderViewEntity)
    {
        return createAABB(x, y, z, x + 1, y + 1, z + 1, expand, partialTicks, renderViewEntity);
    }

    /**
     * Creates an AABB for rendering purposes, which is offset by the render view entity's movement and current partialTicks
     */
    public static AxisAlignedBB createAABB(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, double expand, double partialTicks, Entity entity)
    {
        double dx = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double dy = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double dz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        return new AxisAlignedBB(   minX - dx - expand, minY - dy - expand, minZ - dz - expand,
                                    maxX - dx + expand, maxY - dy + expand, maxZ - dz + expand);
    }
}
