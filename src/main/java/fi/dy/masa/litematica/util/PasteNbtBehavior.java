package fi.dy.masa.litematica.util;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum PasteNbtBehavior implements IConfigOptionListEntry
{
    NONE            ("none",              "litematica.gui.label.paste_nbt_behavior.none"),
    PLACE_MODIFY    ("place_data_modify", "litematica.gui.label.paste_nbt_behavior.place_data_modify"),
    PLACE_CLONE     ("place_clone",       "litematica.gui.label.paste_nbt_behavior.place_clone");

    private final String configString;
    private final String translationKey;

    PasteNbtBehavior(String configString, String translationKey)
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
    public PasteNbtBehavior fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static PasteNbtBehavior fromStringStatic(String name)
    {
        for (PasteNbtBehavior val : PasteNbtBehavior.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PasteNbtBehavior.NONE;
    }
}
