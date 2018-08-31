package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;
import fi.dy.masa.malilib.config.gui.ConfigPanelHotkeysBase;
import fi.dy.masa.malilib.config.gui.ConfigPanelSub;

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
        String modId = Reference.MOD_ID;
        this.addSubPanel(new ConfigPanelSub(modId, "Generic", Configs.Generic.OPTIONS.toArray(new IConfigValue[Configs.Generic.OPTIONS.size()]), this));
        this.addSubPanel(new ConfigPanelSub(modId, "Visuals", Configs.Visuals.OPTIONS.toArray(new IConfigValue[Configs.Visuals.OPTIONS.size()]), this));
        this.addSubPanel(new ConfigPanelSub(modId, "Colors", Configs.Colors.OPTIONS.toArray(new IConfigValue[Configs.Colors.OPTIONS.size()]), this));
        this.addSubPanel(new ConfigPanelHotkeysBase(modId, "Generic Hotkeys", Hotkeys.HOTKEY_LIST, this));
    }
}
