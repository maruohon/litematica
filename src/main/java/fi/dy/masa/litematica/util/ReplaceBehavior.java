package fi.dy.masa.litematica.util;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum ReplaceBehavior implements IConfigOptionListEntry
{
    NONE            ("none",            "litematica.gui.label.replace_behavior.none"),
    ALL             ("all",             "litematica.gui.label.replace_behavior.all"),
    WITH_NON_AIR    ("with_non_air",    "litematica.gui.label.replace_behavior.with_non_air");

    private final String configString;
    private final String translationKey;

    private ReplaceBehavior(String configString, String translationKey)
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
        return StringUtils.translate(this.translationKey);
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
    public ReplaceBehavior fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static ReplaceBehavior fromStringStatic(String name)
    {
        for (ReplaceBehavior val : ReplaceBehavior.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return ReplaceBehavior.NONE;
    }
}
