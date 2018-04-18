package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.render.OverlayRenderer;
import net.minecraft.client.Minecraft;

public class RenderEventHandler
{
    private static final RenderEventHandler INSTANCE = new RenderEventHandler();
    private boolean enabled;

    public static RenderEventHandler getInstance()
    {
        return INSTANCE;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void toggleEnabled()
    {
        this.enabled = ! this.enabled;
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void onRenderWorldLast(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enabled && mc.player != null)
        {
            // FIXME testing
            OverlayRenderer.getInstance().renderSelectionBox(InputEventHandler.selection.getPos1(), InputEventHandler.selection.getPos2());
        }
    }

    public void onRenderGameOverlayPost(float partialTicks)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (this.enabled &&
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
