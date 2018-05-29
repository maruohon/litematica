package fi.dy.masa.litematica.util;

import net.minecraft.client.resources.I18n;

public enum OperationMode
{
    AREA_SELECTION      ("litematica.operation_mode.name.area_selection"),
    PLACEMENT           ("litematica.operation_mode.name.placement"),
    FILL                ("litematica.operation_mode.name.fill"),
    REPLACE             ("litematica.operation_mode.name.replace"),
    CLONE_SOURCE        ("litematica.operation_mode.name.clone_source"),
    CLONE_DESTINATION   ("litematica.operation_mode.name.clone_destination"),
    MOVE_SOURCE         ("litematica.operation_mode.name.move_source"),
    MOVE_DESTINATION    ("litematica.operation_mode.name.move_destination"),
    STACK               ("litematica.operation_mode.name.stack"),
    DELETE              ("litematica.operation_mode.name.delete");

    private final String unlocName;

    private OperationMode(String unlocName)
    {
        this.unlocName = unlocName;
    }

    public String getName()
    {
        return I18n.format(this.unlocName);
    }

    public OperationMode cycle(boolean forward)
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
}
