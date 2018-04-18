package fi.dy.masa.litematica.render;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.util.Vec3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class OverlayRenderer
{
    private static final OverlayRenderer INSTANCE = new OverlayRenderer();
    private final Minecraft mc;
    private Vec3f colorPos1 = new Vec3f(1f, 0.0625f, 0.0625f);
    private Vec3f colorPos2 = new Vec3f(0.0625f, 0.0625f, 1f);
    private Vec3f colorX = new Vec3f(   1f, 0.25f, 0.25f);
    private Vec3f colorY = new Vec3f(0.25f,    1f, 0.25f);
    private Vec3f colorZ = new Vec3f(0.25f, 0.25f,    1f);

    private OverlayRenderer()
    {
        this.mc = Minecraft.getMinecraft();
    }

    public static OverlayRenderer getInstance()
    {
        return INSTANCE;
    }

    public void renderSelectionBox(@Nullable BlockPos pos1, @Nullable BlockPos pos2)
    {
        if (pos1 == null && pos2 == null)
        {
            return;
        }

        Entity renderViewEntity = this.mc.getRenderViewEntity();
        float partialTicks = this.mc.getRenderPartialTicks();
        float expand = 0.001f;
        float lineWidthArea = 1.5f;
        float lineWidthBlockBox = 2f;

        GlStateManager.depthMask(true);
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.pushMatrix();

        if (pos1 != null && pos2 != null && pos1.equals(pos2) == false)
        {
            RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, this.colorPos1, renderViewEntity, partialTicks);
            RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, this.colorPos2, renderViewEntity, partialTicks);

            RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, this.colorX, this.colorY, this.colorZ, renderViewEntity, partialTicks);
        }
        else if (pos1 != null)
        {
            RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, this.colorPos1, renderViewEntity, partialTicks);
        }
        else if (pos2 != null)
        {
            RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, this.colorPos2, renderViewEntity, partialTicks);
        }

        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
    }
}
