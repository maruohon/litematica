package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;
import fi.dy.masa.malilib.config.gui.ConfigPanelSub;

public class ConfigPanelSubLitematica extends ConfigPanelSub
{
    public ConfigPanelSubLitematica(String title, IConfigValue[] configs, ConfigPanelBase parent)
    {
        super(title, parent);

        this.configs = configs;
    }

    @Override
    protected void onSettingsChanged()
    {
        Configs.save();
        Configs.load();
    }
}
