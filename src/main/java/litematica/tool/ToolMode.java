package litematica.tool;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.state.IBlockState;

import malilib.util.StringUtils;
import malilib.util.game.wrap.GameWrap;
import litematica.data.DataManager;

public enum ToolMode
{
    AREA_SELECTION      ("litematica.name.tool_mode.area_selection",        false, false),
    SCHEMATIC_PLACEMENT ("litematica.name.tool_mode.schematic_placement",   false, true),
    FILL                ("litematica.name.tool_mode.fill",                  true, false, true, false),
    REPLACE_BLOCK       ("litematica.name.tool_mode.replace_block",         true, false, true, true),
    PASTE_SCHEMATIC     ("litematica.name.tool_mode.paste_schematic",       true, true),
    GRID_PASTE          ("litematica.name.tool_mode.grid_paste",            true, true),
    MOVE                ("litematica.name.tool_mode.move",                  true, false),
    DELETE              ("litematica.name.tool_mode.delete",                true, false),
    SCHEMATIC_EDIT      ("litematica.name.tool_mode.schematic_edit",        false, true, true, false);

    private final String translationKey;
    private final boolean creativeOnly;
    private final boolean usesSchematic;
    private final boolean usesBlockPrimary;
    private final boolean usesBlockSecondary;

    @Nullable private IBlockState blockPrimary;
    @Nullable private IBlockState blockSecondary;

    public static final ImmutableList<ToolMode> VALUES = ImmutableList.copyOf(values());

    ToolMode(String translationKey, boolean creativeOnly, boolean usesSchematic)
    {
        this(translationKey, creativeOnly, usesSchematic, false, false);
    }

    ToolMode(String translationKey, boolean creativeOnly, boolean usesSchematic, boolean usesBlockPrimary, boolean usesBlockSecondary)
    {
        this.translationKey = translationKey;
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

    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    public ToolMode cycle(boolean forward)
    {
        ToolMode[] values = ToolMode.values();
        final boolean isCreative = GameWrap.isCreativeMode();
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
