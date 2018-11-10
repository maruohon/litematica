package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;
import fi.dy.masa.malilib.config.gui.GuiModConfigs;

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
        this.addSubPanel((new GuiModConfigs(modId, "Generic", Configs.Generic.OPTIONS)).setConfigWidth(160));
        this.addSubPanel((new GuiModConfigs(modId, "Info Overlays", Configs.InfoOverlays.OPTIONS)).setConfigWidth(160));
        this.addSubPanel((new GuiModConfigs(modId, "Visuals", Configs.Visuals.OPTIONS)).setConfigWidth(120));
        this.addSubPanel((new GuiModConfigs(modId, "Colors", Configs.Colors.OPTIONS)).setConfigWidth(100));
        this.addSubPanel((new GuiModConfigs(modId, "Generic Hotkeys", Hotkeys.HOTKEY_LIST)).setConfigWidth(210));
    }
}
