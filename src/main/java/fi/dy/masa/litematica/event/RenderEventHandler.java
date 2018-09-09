package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.ToolHud;
import net.minecraft.client.Minecraft;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();

    public static RenderEventHandler getInstance()
    {
        return INSTANCE;
    }

    public void onRenderWorldLast(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            if (Configs.Visuals.ENABLE_GHOST_BLOCK_RENDERING.getBooleanValue())
            {
                LitematicaRenderer.getInstance().renderSchematicWorld(partialTicks);
            }

            if (Configs.Visuals.ENABLE_SELECTION_BOXES_RENDERING.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSelectionAreas(partialTicks);
            }

            if (Configs.Visuals.ENABLE_VERIFIER_OVERLAY_RENDERING.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicMismatches(partialTicks);
            }
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            mc.currentScreen == null && mc.gameSettings.showDebugInfo == false && mc.player != null)
        {
            ToolHud.getInstance().renderHud();
            InfoHud.getInstance().renderHud();
            OverlayRenderer.renderHoverInfo(mc);
        }
    }
}
