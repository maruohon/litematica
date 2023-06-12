package fi.dy.masa.litematica.render.infohud;

import fi.dy.masa.malilib.config.HudAlignment;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public interface IInfoHudRenderer
{
    /**
     * Return true if this renderer should render its text in the indicated phase
     * @return boolean
     */
    boolean getShouldRenderText(RenderPhase phase);

    /**
     * Return true if this renderer should render its custom content via render()
     * @return boolean
     */
    default boolean getShouldRenderCustom()
    {
        return false;
    }

    /**
     * Whether this renderer should also be rendered when GUIs are open
     * @return boolean
     */
    default boolean shouldRenderInGuis()
    {
        return false;
    }

    /**
     * Returns the text lines rendered by the InfoHud, if any
     * @return boolean
     */
    List<String> getText(RenderPhase phase);

    /**
     * Render any custom content on the HUD.
     * @param yOffset The starting y offset from the edge of the screen indicated by alignment
     * @param alignment the screen position to render at
     * @return the required y height used up for the rendered content
     */
    default int render(int xOffset, int yOffset, HudAlignment alignment, DrawContext context)
    {
        return 0;
    }
}
