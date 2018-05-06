package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.OverlayRenderer;
import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();
    private boolean enableRendering;
    private boolean renderSelections;
    private boolean renderSchematics;
    private SchematicaSchematic schematic;

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

    public void setSchematic(SchematicaSchematic schematic)
    {
        this.schematic = schematic;
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

            // FIXME testing
            if (this.schematic != null && InputEventHandler.selection.getPos1() != null)
            {
                Entity renderViewEntity = mc.getRenderViewEntity();
                RenderUtils.renderGhostBlocks(this.schematic.getBlocks(), this.schematic.getSize(), InputEventHandler.selection.getPos1(), renderViewEntity, partialTicks);
            }
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enableRendering &&
            mc.gameSettings.showDebugInfo == false &&
            mc.player != null
            // &&
            //(ConfigsGeneric.REQUIRE_SNEAK.getBooleanValue() == false || mc.player.isSneaking()) &&
            //(ConfigsGeneric.REQUIRE_HOLDING_KEY.getBooleanValue() == false || InputEventHandler.isRequiredKeyActive())
            )
        {
            //this.renderText(mc, ConfigsGeneric.TEXT_POS_X.getIntegerValue(), ConfigsGeneric.TEXT_POS_Y.getIntegerValue(), this.lines);
        }
    }
}
