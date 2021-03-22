package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.gui.config.ModConfigScreen;
import fi.dy.masa.malilib.gui.config.liteloader.BaseConfigPanel;
import fi.dy.masa.malilib.util.data.ModInfo;

public class LitematicaConfigPanel extends BaseConfigPanel
{
    @Override
    protected String getPanelTitlePrefix()
    {
        return Reference.MOD_NAME + " options";
    }

    @Override
    protected void createSubPanels()
    {
        ModInfo modInfo = Reference.MOD_INFO;
        this.addSubPanel((new ModConfigScreen(modInfo, Configs.Generic.OPTIONS, "litematica.gui.button.config_gui.generic")).setConfigElementsWidth(160));
        this.addSubPanel((new ModConfigScreen(modInfo, Configs.InfoOverlays.OPTIONS, "litematica.gui.button.config_gui.info_overlays")).setConfigElementsWidth(160));
        this.addSubPanel((new ModConfigScreen(modInfo, Configs.Visuals.OPTIONS, "litematica.gui.button.config_gui.visuals")).setConfigElementsWidth(120));
        this.addSubPanel((new ModConfigScreen(modInfo, Configs.Colors.OPTIONS, "litematica.gui.button.config_gui.colors")).setConfigElementsWidth(100));
        this.addSubPanel((new ModConfigScreen(modInfo, Hotkeys.HOTKEY_LIST, "litematica.gui.button.config_gui.hotkeys")).setConfigElementsWidth(210));
    }
}
