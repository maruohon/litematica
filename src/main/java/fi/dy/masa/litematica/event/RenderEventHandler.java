package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.ToolHud;
import net.minecraft.client.Minecraft;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();
    private boolean enableRendering = true;
    private boolean renderMismatches = true;
    private boolean renderSchematics = true;
    private boolean renderSelections = true;

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

    public boolean toggleRenderMismatches()
    {
        this.renderMismatches = ! this.renderMismatches;
        return this.renderMismatches;
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
            }

            if (this.renderSelections)
            {
                OverlayRenderer.getInstance().renderSelectionAreas();
            }

            if (this.renderMismatches)
            {
                OverlayRenderer.getInstance().renderSchematicMismatches(partialTicks);
            }
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enableRendering && mc.currentScreen == null && mc.gameSettings.showDebugInfo == false && mc.player != null)
        {
            ToolHud.getInstance().renderHud();
            InfoHud.getInstance().renderHud();

            if (this.renderMismatches)
            {
                OverlayRenderer.renderHoverInfoForBlockMismatch(mc);
            }
        }
    }
}
