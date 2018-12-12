package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.Minecraft;

public class RenderHandler implements IRenderer
{
    @Override
    public void onRenderWorldLast(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isValid() && Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

            if (Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert)
            {
                LitematicaRenderer.getInstance().renderSchematicWorld(partialTicks);
            }

            if (Configs.Visuals.ENABLE_SELECTION_BOXES_RENDERING.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSelectionAreas(partialTicks);
            }

            if (Configs.InfoOverlays.ENABLE_VERIFIER_OVERLAY_RENDERING.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicMismatches(partialTicks);
            }
        }
    }

    @Override
    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
            mc.currentScreen == null && mc.gameSettings.showDebugInfo == false && mc.player != null)
        {
            ToolHud.getInstance().renderHud();
            InfoHud.getInstance().renderHud();
            OverlayRenderer.getInstance().renderHoverInfo(mc);
        }
    }
}
