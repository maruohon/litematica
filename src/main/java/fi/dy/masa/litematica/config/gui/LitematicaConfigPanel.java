package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.gui.config.GuiModConfigs;
import fi.dy.masa.malilib.gui.config.liteloader.ConfigPanelBase;

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
        this.addSubPanel((new GuiModConfigs(modId, Configs.Generic.OPTIONS, "litematica.gui.button.config_gui.generic")).setConfigWidth(160));
        this.addSubPanel((new GuiModConfigs(modId, Configs.InfoOverlays.OPTIONS, "litematica.gui.button.config_gui.info_overlays")).setConfigWidth(160));
        this.addSubPanel((new GuiModConfigs(modId, Configs.Visuals.OPTIONS, "litematica.gui.button.config_gui.visuals")).setConfigWidth(120));
        this.addSubPanel((new GuiModConfigs(modId, Configs.Colors.OPTIONS, "litematica.gui.button.config_gui.colors")).setConfigWidth(100));
        this.addSubPanel((new GuiModConfigs(modId, Hotkeys.HOTKEY_LIST, "litematica.gui.button.config_gui.hotkeys")).setConfigWidth(210));
    }
}
