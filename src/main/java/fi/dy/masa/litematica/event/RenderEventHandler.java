package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.data.DataManager;
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

        if (DataManager.isRenderingEnabled() && mc.player != null)
        {
            if (DataManager.renderSchematics())
            {
                LitematicaRenderer.getInstance().renderSchematicWorld(partialTicks);
            }

            if (DataManager.renderSelections())
            {
                OverlayRenderer.getInstance().renderSelectionAreas(partialTicks);
            }

            if (DataManager.renderMismatches())
            {
                OverlayRenderer.getInstance().renderSchematicMismatches(partialTicks);
            }
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (DataManager.isRenderingEnabled() && mc.currentScreen == null && mc.gameSettings.showDebugInfo == false && mc.player != null)
        {
            ToolHud.getInstance().renderHud();
            InfoHud.getInstance().renderHud();
            OverlayRenderer.renderHoverInfo(mc);
        }
    }
}
