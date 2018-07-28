package fi.dy.masa.litematica.util;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;

public enum OperationMode
{
    AREA_SELECTION      ("litematica.operation_mode.name.area_selection", false),
    SCHEMATIC_PLACEMENT ("litematica.operation_mode.name.schematic_placement", true),
    FILL                ("litematica.operation_mode.name.fill", false),
    REPLACE_BLOCK       ("litematica.operation_mode.name.replace_block", false),
    PASTE_SCHEMATIC     ("litematica.operation_mode.name.paste_schematic", true),
    GRID_PASTE          ("litematica.operation_mode.name.grid_paste", true),
    DELETE              ("litematica.operation_mode.name.delete", false);

    private final String unlocName;
    private final boolean usesSchematic;

    private OperationMode(String unlocName, boolean usesSchematic)
    {
        this.unlocName = unlocName;
        this.usesSchematic = usesSchematic;
    }

    public boolean getUsesSchematic()
    {
        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection()
    {
        return this.usesSchematic == false;
    }

    public String getName()
    {
        return I18n.format(this.unlocName);
    }

    public OperationMode cycle(EntityPlayer player, boolean forward)
    {
        int id = this.ordinal();
        int numModes = player.capabilities.isCreativeMode ? values().length : 2;

        if (forward)
        {
            if (++id >= numModes)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = numModes - 1;
            }
        }

        return values()[id % values().length];
    }
}
