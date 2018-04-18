package fi.dy.masa.litematica.render;

import org.lwjgl.opengl.GL11;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.Vec3f;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class RenderUtils
{
    public static void renderBlockOutline(BlockPos pos, float expand, float lineWidth,
            Vec3f color, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.glLineWidth(lineWidth);

        AxisAlignedBB aabb = PositionUtils.createAABB(pos.getX(), pos.getY(), pos.getZ(), expand, partialTicks, renderViewEntity);
        RenderGlobal.drawSelectionBoundingBox(aabb, color.x, color.y, color.z, 1f);
    }

    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
            Vec3f color, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.glLineWidth(lineWidth);

        AxisAlignedBB aabb = PositionUtils.createEnclosingAABB(pos1, pos2, renderViewEntity, partialTicks);
        RenderGlobal.drawSelectionBoundingBox(aabb, color.x, color.y, color.z, 1f);
    }

    public static void renderAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth,
            Vec3f colorX, Vec3f colorY, Vec3f colorZ, Entity renderViewEntity, float partialTicks)
    {
        GlStateManager.glLineWidth(lineWidth);

        AxisAlignedBB aabb = PositionUtils.createEnclosingAABB(pos1, pos2, renderViewEntity, partialTicks);
        drawBoundingBox(aabb, colorX, colorY, colorZ);
    }

    private static void drawBoundingBox(AxisAlignedBB box, Vec3f colorX, Vec3f colorY, Vec3f colorZ)
    {
        drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, colorX, colorY, colorZ);
    }

    private static void drawBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3f colorX, Vec3f colorY, Vec3f colorZ)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        drawBoundingBoxLinesX(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorX);
        drawBoundingBoxLinesY(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorY);
        drawBoundingBoxLinesZ(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, colorZ);

        tessellator.draw();
    }

    private static void drawBoundingBoxLinesX(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3f color)
    {
        buffer.pos(minX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(minX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(minX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(minX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3f color)
    {
        buffer.pos(minX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(minX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(maxX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(minX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(maxX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Vec3f color)
    {
        buffer.pos(minX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(minX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(maxX, minY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, minY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(minX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(minX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();

        buffer.pos(maxX, maxY, minZ).color(color.x, color.y, color.z, 1.0F).endVertex();
        buffer.pos(maxX, maxY, maxZ).color(color.x, color.y, color.z, 1.0F).endVertex();
    }
}
