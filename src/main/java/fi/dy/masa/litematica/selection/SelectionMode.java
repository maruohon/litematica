package fi.dy.masa.litematica.selection;

import fi.dy.masa.malilib.util.StringUtils;

public enum SelectionMode
{
    NORMAL  ("litematica.gui.label.area_selection.mode.normal"),
    SIMPLE  ("litematica.gui.label.area_selection.mode.simple");

    private final String translationKey;

    private SelectionMode(String translationKey)
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

    public static SelectionMode fromString(String name)
    {
        for (SelectionMode mode : SelectionMode.values())
        {
            if (mode.name().equalsIgnoreCase(name))
            {
                return mode;
            }
        }

        return SelectionMode.NORMAL;
    }
}
