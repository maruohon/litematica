package fi.dy.masa.litematica.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.GuiUtils;

public class RenderHandler implements IRenderer
{
    @Override
    public void onRenderWorldLast(float partialTicks, MatrixStack matrices)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            OverlayRenderer.getInstance().renderBoxes(matrices, partialTicks);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(matrices, partialTicks);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(matrices, partialTicks);
            }
        }
    }

    @Override
    public void onRenderGameOverlayPost(float partialTicks)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            // The Info HUD renderers can decide if they want to be rendered in GUIs
            InfoHud.getInstance().renderHud();

            if (GuiUtils.getCurrentScreen() == null)
            {
                ToolHud.getInstance().renderHud();
                OverlayRenderer.getInstance().renderHoverInfo(mc);

                if (GuiSchematicManager.hasPendingPreviewTask())
                {
                    OverlayRenderer.getInstance().renderPreviewFrame(mc);
                }
            }
        }
    }
}
