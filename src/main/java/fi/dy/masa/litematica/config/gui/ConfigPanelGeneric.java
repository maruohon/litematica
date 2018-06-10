package fi.dy.masa.litematica.config.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.gui.ConfigPanelBase;

public class ConfigPanelGeneric extends ConfigPanelSubLitematica
{
    public ConfigPanelGeneric(ConfigPanelBase parent)
    {
        super("Generic", parent);
    }

    @Override
    protected IConfigValue[] getConfigs()
    {
        return Configs.Generic.OPTIONS.toArray(new IConfigValue[Configs.Generic.OPTIONS.size()]);
    }
}
