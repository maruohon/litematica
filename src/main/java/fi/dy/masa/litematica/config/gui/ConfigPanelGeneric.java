package fi.dy.masa.litematica.config.gui;

import java.util.Collection;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.options.ConfigBase;

public class ConfigPanelGeneric extends ConfigPanelSub
{
    public ConfigPanelGeneric(LitematicaConfigPanel parent)
    {
        super("Generic", parent);
    }

    @Override
    protected Collection<ConfigBase> getConfigs()
    {
        return Configs.Generic.OPTIONS;
    }
}
