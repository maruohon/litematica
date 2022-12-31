package litematica.selection;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionListConfigValue;

public class ToolSelectionMode extends BaseOptionListConfigValue
{
    public static final ToolSelectionMode EXPAND  = new ToolSelectionMode("expand",  "litematica.label.area_selection.tool_selection_mode.expand");
    public static final ToolSelectionMode CORNERS = new ToolSelectionMode("corners", "litematica.label.area_selection.tool_selection_mode.corners");

    public static final ImmutableList<ToolSelectionMode> VALUES = ImmutableList.of(CORNERS, EXPAND);

    public ToolSelectionMode(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
