package litematica.selection;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionListConfigValue;

public class CornerSelectionMode extends BaseOptionListConfigValue
{
    public static final CornerSelectionMode EXPAND  = new CornerSelectionMode("expand",  "litematica.hud.area_selection.mode.expand");
    public static final CornerSelectionMode CORNERS = new CornerSelectionMode("corners", "litematica.hud.area_selection.mode.corners");

    public static final ImmutableList<CornerSelectionMode> VALUES = ImmutableList.of(CORNERS, EXPAND);

    public CornerSelectionMode(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
