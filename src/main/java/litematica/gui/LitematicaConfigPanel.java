package litematica.gui;

import malilib.gui.config.liteloader.RedirectingConfigPanel;

public class LitematicaConfigPanel extends RedirectingConfigPanel
{
    public LitematicaConfigPanel()
    {
        super(ConfigScreen::create);
    }
}
