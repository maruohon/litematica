package fi.dy.masa.litematica.selection;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.client.resources.I18n;

public enum CornerSelectionMode implements IConfigOptionListEntry
{
    CORNERS     ("corners",     "litematica.hud.area_selection.mode.corners"),
    EXPAND      ("expand",      "litematica.hud.area_selection.mode.expand");

    private final String configString;
    private final String translationKey;

    private CornerSelectionMode(String configString, String translationKey)
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
    public CornerSelectionMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static CornerSelectionMode fromStringStatic(String name)
    {
        for (CornerSelectionMode mode : CornerSelectionMode.values())
        {
            if (mode.configString.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return CornerSelectionMode.CORNERS;
    }
}
