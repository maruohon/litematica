package fi.dy.masa.litematica.util;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum PlacementDeletionMode implements IConfigOptionListEntry
{
    MATCHING_BLOCK          ("matching_block",      "Matching Block"),
    NON_MATCHING_BLOCK      ("non_matching_block",  "Non-Matching Block"),
    ANY_SCHEMATIC_BLOCK     ("any_schematic_block", "Any Schematic Block"),
    NO_SCHEMATIC_BLOCK      ("no_schematic_block",  "No Schematic Block"),
    ENTIRE_VOLUME           ("entire_volume",       "Entire Volume");

    private final String configString;
    private final String translationKey;

    PlacementDeletionMode(String configString, String translationKey)
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
    public PlacementDeletionMode fromString(String name)
    {
        return fromStringStatic(name);
    }

    public static PlacementDeletionMode fromStringStatic(String name)
    {
        for (PlacementDeletionMode val : PlacementDeletionMode.values())
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return PlacementDeletionMode.ENTIRE_VOLUME;
    }
}
