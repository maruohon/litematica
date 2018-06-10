package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;
import fi.dy.masa.malilib.config.gui.ConfigPanelSub;

public abstract class ConfigPanelSubLitematica extends ConfigPanelSub
{
    public ConfigPanelSubLitematica(String title, ConfigPanelBase parent)
    {
        super(title, parent);
    }

    @Override
    protected void onSettingsChanged()
    {
        Configs.save();
        Configs.load();
    }
}
