package fi.dy.masa.litematica.tool;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;

public enum ToolMode
{
    AREA_SELECTION      ("litematica.operation_mode.name.area_selection", false),
    SCHEMATIC_PLACEMENT ("litematica.operation_mode.name.schematic_placement", true),
    FILL                ("litematica.operation_mode.name.fill", false, true, false),
    REPLACE_BLOCK       ("litematica.operation_mode.name.replace_block", false, true, true),
    PASTE_SCHEMATIC     ("litematica.operation_mode.name.paste_schematic", true),
    GRID_PASTE          ("litematica.operation_mode.name.grid_paste", true),
    DELETE              ("litematica.operation_mode.name.delete", false);

    private final String unlocName;
    private final boolean usesSchematic;
    private final boolean usesBlockPrimary;
    private final boolean usesBlockSecondary;

    @Nullable private IBlockState blockPrimary;
    @Nullable private IBlockState blockSecondary;

    private ToolMode(String unlocName, boolean usesSchematic)
    {
        this(unlocName, usesSchematic, false, false);
    }

    private ToolMode(String unlocName, boolean usesSchematic, boolean usesBlockPrimary, boolean usesBlockSecondary)
    {
        this.unlocName = unlocName;
        this.usesSchematic = usesSchematic;
        this.usesBlockPrimary = usesBlockPrimary;
        this.usesBlockSecondary = usesBlockSecondary;
    }

    public boolean getUsesSchematic()
    {
        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection()
    {
        return this.usesSchematic == false;
    }

    public boolean getUsesBlockPrimary()
    {
        return this.usesBlockPrimary;
    }

    public boolean getUsesBlockSecondary()
    {
        return this.usesBlockSecondary;
    }

    @Nullable
    public IBlockState getPrimaryBlock()
    {
        return this.blockPrimary;
    }

    @Nullable
    public IBlockState getSecondaryBlock()
    {
        return this.blockSecondary;
    }

    public void setPrimaryBlock(@Nullable IBlockState state)
    {
        this.blockPrimary = state;
    }

    public void setSecondaryBlock(@Nullable IBlockState state)
    {
        this.blockSecondary = state;
    }

    public String getName()
    {
        return I18n.format(this.unlocName);
    }

    public ToolMode cycle(EntityPlayer player, boolean forward)
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
