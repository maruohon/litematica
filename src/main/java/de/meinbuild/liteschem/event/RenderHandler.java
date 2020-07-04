package de.meinbuild.liteschem.event;

import de.meinbuild.liteschem.config.Configs;
import de.meinbuild.liteschem.render.OverlayRenderer;
import de.meinbuild.liteschem.render.infohud.InfoHud;
import de.meinbuild.liteschem.render.infohud.ToolHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.gui.GuiSchematicManager;
import de.meinbuild.liteschem.tool.ToolMode;
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
    public void onRenderGameOverlayPost(float partialTicks, MatrixStack matrixStack)
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
