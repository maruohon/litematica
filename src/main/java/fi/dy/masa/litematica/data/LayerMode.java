package fi.dy.masa.litematica.data;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.client.resources.I18n;

public enum LayerMode implements IConfigOptionListEntry
{
    ALL             ("all",             "litematica.gui.label.layer_mode.all"),
    SINGLE_LAYER    ("single_layer",    "litematica.gui.label.layer_mode.single_layer"),
    LAYER_RANGE     ("layer_range",     "litematica.gui.label.layer_mode.layer_range");

    private final String configString;
    private final String translationKey;

    private LayerMode(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return I18n.format(this.translationKey);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public LayerMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static LayerMode fromStringStatic(String name)
    {
        for (LayerMode mode : LayerMode.values())
        {
            if (mode.configString.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return LayerMode.ALL;
    }
}
