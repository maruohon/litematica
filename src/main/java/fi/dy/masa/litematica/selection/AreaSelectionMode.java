package fi.dy.masa.litematica.selection;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;

public enum AreaSelectionMode implements IConfigOptionListEntry
{
    CORNERS     ("Corners"),
    CUBOID      ("Cuboid");

    private final String displayName;

    private AreaSelectionMode(String displayName)
    {
        this.displayName = displayName;
    }

    @Override
    public String getStringValue()
    {
        return this.name().toLowerCase();
    }

    @Override
    public String getDisplayName()
    {
        return this.displayName;
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
        for (AreaSelectionMode al : AreaSelectionMode.values())
        {
            if (al.name().equalsIgnoreCase(name))
            {
                return al;
            }
        }

        return AreaSelectionMode.CORNERS;
    }
}
