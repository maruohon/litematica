package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum EasyPlaceProtocol implements IConfigOptionListEntry
{
    AUTO                ("auto",                  "litematica.gui.label.easy_place_protocol.auto"),
    V3                  ("v3",                    "litematica.gui.label.easy_place_protocol.v3"),
    V2                  ("v2",                    "litematica.gui.label.easy_place_protocol.v2"),
    SLAB_ONLY           ("slabs_only",            "litematica.gui.label.easy_place_protocol.slabs_only"),
    NONE                ("none",                  "litematica.gui.label.easy_place_protocol.none");

    public static final ImmutableList<EasyPlaceProtocol> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    EasyPlaceProtocol(String configString, String translationKey)
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
    public EasyPlaceProtocol fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static EasyPlaceProtocol fromStringStatic(String name)
    {
        for (EasyPlaceProtocol val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return EasyPlaceProtocol.AUTO;
    }
}
