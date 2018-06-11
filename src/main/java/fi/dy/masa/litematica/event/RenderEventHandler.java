package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.ToolHud;
import net.minecraft.client.Minecraft;

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
        }
    }
}
