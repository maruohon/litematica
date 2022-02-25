package fi.dy.masa.litematica.tool;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.util.StringUtils;

public enum ToolMode
{
    AREA_SELECTION      ("litematica.tool_mode.name.area_selection",        false, false),
    SCHEMATIC_PLACEMENT ("litematica.tool_mode.name.schematic_placement",   false, true),
    FILL                ("litematica.tool_mode.name.fill",                  true, false, true, false),
    REPLACE_BLOCK       ("litematica.tool_mode.name.replace_block",         true, false, true, true),
    PASTE_SCHEMATIC     ("litematica.tool_mode.name.paste_schematic",       true, true),
    GRID_PASTE          ("litematica.tool_mode.name.grid_paste",            true, true),
    MOVE                ("litematica.tool_mode.name.move",                  true, false),
    DELETE              ("litematica.tool_mode.name.delete",                true, false),
    REBUILD             ("litematica.tool_mode.name.rebuild",               false, true, true, false);

    private final String unlocName;
    private final boolean creativeOnly;
    private final boolean usesSchematic;
    private final boolean usesBlockPrimary;
    private final boolean usesBlockSecondary;

    @Nullable private IBlockState blockPrimary;
    @Nullable private IBlockState blockSecondary;

    public static final ImmutableList<ToolMode> VALUES = ImmutableList.copyOf(values());

    ToolMode(String unlocName, boolean creativeOnly, boolean usesSchematic)
    {
        this(unlocName, creativeOnly, usesSchematic, false, false);
    }

    ToolMode(String unlocName, boolean creativeOnly, boolean usesSchematic, boolean usesBlockPrimary, boolean usesBlockSecondary)
    {
        this.unlocName = unlocName;
        this.creativeOnly = creativeOnly;
        this.usesSchematic = usesSchematic;
        this.usesBlockPrimary = usesBlockPrimary;
        this.usesBlockSecondary = usesBlockSecondary;
    }

    public boolean getUsesSchematic()
    {
        if (this == ToolMode.DELETE && ToolModeData.DELETE.getUsePlacement())
        {
            return true;
        }

        return this.usesSchematic;
    }

    public boolean getUsesAreaSelection()
    {
        return this.getUsesSchematic() == false || DataManager.getSchematicProjectsManager().hasProjectOpen();
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
        return StringUtils.translate(this.unlocName);
    }

    public ToolMode cycle(EntityPlayer player, boolean forward)
    {
        ToolMode[] values = ToolMode.values();
        final boolean isCreative = player.capabilities.isCreativeMode;
        final int numModes = values.length;
        final int inc = forward ? 1 : -1;
        int nextId = this.ordinal() + inc;

        for (int i = 0; i < numModes; ++i)
        {
            if (nextId < 0)
            {
                nextId = numModes - 1;
            }
            else if (nextId >= numModes)
            {
                nextId = 0;
            }

            ToolMode mode = values[nextId];

            if (isCreative || mode.creativeOnly == false)
            {
                return mode;
            }

            nextId += inc;
        }

        return this;
    }

    public static ToolMode fromString(String name)
    {
        ToolMode mode = ToolMode.valueOf(name);

        if (mode == null)
        {
            mode = AREA_SELECTION;
        }

        return mode;
    }
}
