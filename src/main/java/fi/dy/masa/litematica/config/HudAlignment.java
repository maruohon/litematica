package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.interfaces.IConfigOptionListEntry;

public enum HudAlignment implements IConfigOptionListEntry
{
    TOP_LEFT        ("Top Left"),
    TOP_RIGHT       ("Top Right"),
    BOTTOM_LEFT     ("Bottom Left"),
    BOTTOM_RIGHT    ("Bottom Right"),
    CENTER          ("Center");

    private final String displayName;

    private HudAlignment(String displayName)
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
    public int getOrdinalValue()
    {
        return this.ordinal();
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
    public HudAlignment fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static HudAlignment fromStringStatic(String name)
    {
        for (HudAlignment al : HudAlignment.values())
        {
            if (al.name().equalsIgnoreCase(name))
            {
                return al;
            }
        }

        return HudAlignment.TOP_LEFT;
    }
}
