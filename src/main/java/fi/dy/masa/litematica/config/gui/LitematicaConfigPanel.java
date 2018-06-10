package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;

public class LitematicaConfigPanel extends ConfigPanelBase
{
    @Override
    protected String getPanelTitlePrefix()
    {
        return Reference.MOD_NAME + " options";
    }

    @Override
    protected void createSubPanels()
    {
        this.addSubPanel(new ConfigPanelGeneric(this));
        this.addSubPanel(new ConfigPanelGenericHotkeys(this));
    }
}
