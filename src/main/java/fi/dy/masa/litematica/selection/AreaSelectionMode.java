package fi.dy.masa.litematica.selection;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.client.resources.I18n;

public enum AreaSelectionMode implements IConfigOptionListEntry
{
    CORNERS     ("corners",     "litematica.hud.area_selection.mode.corners"),
    CUBOID      ("cuboid",      "litematica.hud.area_selection.mode.cuboid");

    private final String configString;
    private final String unlocName;

    private AreaSelectionMode(String configString, String displayName)
    {
        this.configString = configString;
        this.unlocName = displayName;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return I18n.format(this.unlocName);
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
    public AreaSelectionMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static AreaSelectionMode fromStringStatic(String name)
    {
        for (AreaSelectionMode mode : AreaSelectionMode.values())
        {
            if (mode.configString.equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return AreaSelectionMode.CORNERS;
    }
}
