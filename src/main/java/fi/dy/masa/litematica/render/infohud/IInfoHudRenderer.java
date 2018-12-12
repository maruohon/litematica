package fi.dy.masa.litematica.render.infohud;

import java.util.List;
import fi.dy.masa.malilib.config.HudAlignment;

public interface IInfoHudRenderer
{
    /**
     * Whether or not this renderer is currently enabled
     * @return
     */
    boolean getShouldRender();

    /**
     * Returns the text lines rendered by the InfoHud, if any
     * @return
     */
    List<String> getText();

    /**
     * Render any custom content on the HUD.
     * @param yOffset The starting y offset from the edge of the screen indicated by alignment
     * @param alignment the screen position to render at
     * @return the required y height used up for the rendered content
     */
    default int render(int xOffset, int yOffset, HudAlignment alignment)
    {
        return 0;
    }
}
