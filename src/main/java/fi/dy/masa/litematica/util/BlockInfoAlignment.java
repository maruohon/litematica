package fi.dy.masa.litematica.util;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BlockInfoAlignment implements IConfigOptionListEntry
{
    CENTER      ("center",      "litematica.label.alignment.center"),
    TOP_CENTER  ("top_center",  "litematica.label.alignment.top_center");

    private final String configString;
    private final String unlocName;

    private BlockInfoAlignment(String configString, String unlocName)
    {
        this.configString = configString;
        this.unlocName = unlocName;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.unlocName);
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
    public BlockInfoAlignment fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static BlockInfoAlignment fromStringStatic(String name)
    {
        for (BlockInfoAlignment aligment : BlockInfoAlignment.values())
        {
            if (aligment.configString.equalsIgnoreCase(name))
            {
                return aligment;
            }
        }

        return BlockInfoAlignment.CENTER;
    }
}
