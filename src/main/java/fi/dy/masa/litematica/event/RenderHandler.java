package fi.dy.masa.litematica.event;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
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
    public void onRenderWorldLast(MatrixStack matrices, Matrix4f projMatrix)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            OverlayRenderer.getInstance().renderBoxes(matrices);

            if (Configs.InfoOverlays.VERIFIER_OVERLAY_ENABLED.getBooleanValue())
            {
                OverlayRenderer.getInstance().renderSchematicVerifierMismatches(matrices);
            }

            if (DataManager.getToolMode() == ToolMode.REBUILD)
            {
                OverlayRenderer.getInstance().renderSchematicRebuildTargetingOverlay(matrices);
            }
        }
    }

    @Override
    public void onRenderGameOverlayPost(MatrixStack matrixStack)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && mc.player != null)
        {
            // The Info HUD renderers can decide if they want to be rendered in GUIs
            InfoHud.getInstance().renderHud(matrixStack);

            if (GuiUtils.getCurrentScreen() == null)
            {
                ToolHud.getInstance().renderHud(matrixStack);
                OverlayRenderer.getInstance().renderHoverInfo(mc, matrixStack);

                if (GuiSchematicManager.hasPendingPreviewTask())
                {
                    OverlayRenderer.getInstance().renderPreviewFrame(mc);
                }
            }
        }
    }
}
