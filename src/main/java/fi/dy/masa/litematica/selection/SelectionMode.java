package fi.dy.masa.litematica.selection;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.util.StringUtils;

public enum SelectionMode
{
    NORMAL  ("litematica.label.area_selection.mode.multi_region"),
    SIMPLE  ("litematica.label.area_selection.mode.simple");

    public static final ImmutableList<SelectionMode> VALUES = ImmutableList.copyOf(values());

    private final String translationKey;

    SelectionMode(String translationKey)
    {
        this.translationKey = translationKey;
    }

    public String getTranslationKey()
    {
        return this.translationKey;
    }

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    public SelectionMode cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= 2)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = 2 - 1;
            }
        }

        return values()[id % 2];
    }

    public static SelectionMode fromString(String name)
    {
        for (SelectionMode mode : VALUES)
        {
            if (mode.name().equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return SelectionMode.NORMAL;
    }
}
