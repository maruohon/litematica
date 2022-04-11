package fi.dy.masa.litematica.selection;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;

public class SelectionMode extends BaseOptionListConfigValue
{
    public static final SelectionMode SIMPLE       = new SelectionMode("simple",       "litematica.label.area_selection.mode.simple");
    public static final SelectionMode MULTI_REGION = new SelectionMode("multi_region", "litematica.label.area_selection.mode.multi_region");

    public static final ImmutableList<SelectionMode> VALUES = ImmutableList.of(SIMPLE, MULTI_REGION);

    public SelectionMode(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
