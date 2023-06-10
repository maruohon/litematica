package fi.dy.masa.litematica.render.infohud;

import java.util.List;

import net.minecraft.client.gui.DrawContext;

import fi.dy.masa.malilib.config.HudAlignment;

public interface IInfoHudRenderer
{
    /**
     * Return true if this renderer should render its text in the indicated phase
     * @return
     */
    boolean getShouldRenderText(RenderPhase phase);

    /**
     * Return true if this renderer should render its custom content via render()
     * @return
     */
    default boolean getShouldRenderCustom()
    {
        return false;
    }

    /**
     * Whether or not this renderer should also be rendered when GUIs are open
     * @return
     */
    default boolean shouldRenderInGuis()
    {
        return false;
    }

    /**
     * Returns the text lines rendered by the InfoHud, if any
     * @return
     */
    List<String> getText(RenderPhase phase);

    /**
     * Render any custom content on the HUD.
     * @param yOffset The starting y offset from the edge of the screen indicated by alignment
     * @param alignment the screen position to render at
     * @return the required y height used up for the rendered content
     */
    default int render(int xOffset, int yOffset, HudAlignment alignment, DrawContext drawContext)
    {
        return 0;
    }
}
