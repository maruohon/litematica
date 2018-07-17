package fi.dy.masa.litematica.event;

import java.util.List;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.ToolHud;
import fi.dy.masa.litematica.util.RayTraceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();
    private boolean enableRendering;
    private boolean renderSelections;
    private boolean renderSchematics;

    public static RenderEventHandler getInstance()
    {
        return INSTANCE;
    }

    public void setEnabled(boolean enabled)
    {
        this.enableRendering = enabled;
    }

    public boolean toggleAllRenderingEnabled()
    {
        this.enableRendering = ! this.enableRendering;
        return this.enableRendering;
    }

    public boolean toggleRenderSelectionBoxes()
    {
        this.renderSelections = ! this.renderSelections;
        return this.renderSelections;
    }

    public boolean toggleRenderSchematics()
    {
        this.renderSchematics = ! this.renderSchematics;
        return this.renderSchematics;
    }

    public boolean isEnabled()
    {
        return this.enableRendering;
    }

    public void onRenderWorldLast(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enableRendering && mc.player != null)
        {
            if (this.renderSchematics)
            {
                LitematicaRenderer.getInstance().renderSchematicWorld();

                List<BlockPos> posList = DataManager.getSelectedMismatchPositions();
                BlockPos posLook = RayTraceUtils.traceToPositions(mc.world, posList, mc.player, 5);
                OverlayRenderer.getInstance().renderSchematicMismatches(posList, posLook, partialTicks);
            }

            OverlayRenderer.getInstance().renderSelectionAreas();
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enableRendering && mc.currentScreen == null && mc.gameSettings.showDebugInfo == false && mc.player != null)
        {
            ToolHud.getInstance().renderHud();
            InfoHud.getInstance().renderHud();
            OverlayRenderer.renderHoverInfoForBlockMismatch(mc);
        }
    }
}
