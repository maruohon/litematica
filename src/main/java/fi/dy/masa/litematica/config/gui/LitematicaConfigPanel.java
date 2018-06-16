package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;
import fi.dy.masa.malilib.config.gui.ConfigPanelHotkeysBase;

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
        this.addSubPanel(new ConfigPanelSubLitematica("Generic", Configs.Generic.OPTIONS.toArray(new IConfigValue[Configs.Generic.OPTIONS.size()]), this));
        this.addSubPanel(new ConfigPanelSubLitematica("Visuals", Configs.Visuals.OPTIONS.toArray(new IConfigValue[Configs.Visuals.OPTIONS.size()]), this));
        this.addSubPanel(new ConfigPanelHotkeysBase("Generic Hotkeys", Hotkeys.values(), this));
    }
}
