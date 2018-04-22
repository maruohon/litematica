package fi.dy.masa.litematica.render;

import fi.dy.masa.litematica.selection.Selection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
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
    private Vec3f colorArea = new Vec3f(1f, 1f, 1f);
    private Vec3f colorSelectedCorner = new Vec3f(0f, 1f, 1f);
    private Vec3f colorAreaOrigin = new Vec3f(1f, 0x90 / 255f, 0x10 / 255f);

    private OverlayRenderer()
    {
        this.mc = Minecraft.getMinecraft();
    }

    public static OverlayRenderer getInstance()
    {
        return INSTANCE;
    }

    public void renderSelectionAreas()
    {
        SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
        Selection area = sm.getCurrentSelection();

        if (area != null)
        {
            Entity renderViewEntity = this.mc.getRenderViewEntity();
            float partialTicks = this.mc.getRenderPartialTicks();
            float expand = 0.001f;
            float lineWidthBlockBox = 2f;
            float lineWidthArea = 1.5f;
            Box currentBox = area.getSelectedSelectionBox();

            GlStateManager.depthMask(true);
            GlStateManager.disableLighting();
            GlStateManager.enableCull();
            GlStateManager.disableTexture2D();
            GlStateManager.pushMatrix();

            for (Box box : area.getAllSelectionsBoxes())
            {
                this.renderSelectionBox(box, box == currentBox, expand, lineWidthBlockBox, lineWidthArea, renderViewEntity, partialTicks);
            }

            if (area.getOrigin() != null)
            {
                RenderUtils.renderBlockOutline(area.getOrigin(), expand, lineWidthBlockBox, this.colorAreaOrigin, renderViewEntity, partialTicks);
            }

            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
        }
    }

    public void renderSelectionBox(Box box, boolean selected, float expand,
            float lineWidthBlockBox, float lineWidthArea, Entity renderViewEntity, float partialTicks)
    {
        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        if (pos1 == null && pos2 == null)
        {
            return;
        }

        //float wb1 = box.getSelectedCorner() == Corner.CORNER_1 ? lineWidthBlockBox * 2 : lineWidthBlockBox;
        //float wb2 = box.getSelectedCorner() == Corner.CORNER_2 ? lineWidthBlockBox * 2 : lineWidthBlockBox;
        Vec3f color1 = box.getSelectedCorner() == Corner.CORNER_1 ? this.colorSelectedCorner : this.colorPos1;
        Vec3f color2 = box.getSelectedCorner() == Corner.CORNER_2 ? this.colorSelectedCorner : this.colorPos2;

        if (pos1 != null && pos2 != null && pos1.equals(pos2) == false)
        {
            if (selected)
            {
                RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, this.colorX, this.colorY, this.colorZ, renderViewEntity, partialTicks);
            }
            else
            {
                RenderUtils.renderAreaOutline(pos1, pos2, lineWidthArea, this.colorArea, this.colorArea, this.colorArea, renderViewEntity, partialTicks);
            }
        }

        if (pos1 != null)
        {
            RenderUtils.renderBlockOutline(pos1, expand, lineWidthBlockBox, color1, renderViewEntity, partialTicks);
        }

        if (pos2 != null)
        {
            RenderUtils.renderBlockOutline(pos2, expand, lineWidthBlockBox, color2, renderViewEntity, partialTicks);
        }
    }
}
