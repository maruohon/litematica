package fi.dy.masa.litematica.gui;

import fi.dy.masa.malilib.gui.config.liteloader.RedirectingConfigPanel;

public class LitematicaConfigPanel extends RedirectingConfigPanel
{
    public LitematicaConfigPanel()
    {
        super(ConfigScreen::create);
    }
}
