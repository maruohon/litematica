package litematica.selection;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionListConfigValue;

public class AreaSelectionType extends BaseOptionListConfigValue
{
    public static final AreaSelectionType SIMPLE       = new AreaSelectionType("simple",       "litematica.label.area_selection.selection_type.simple");
    public static final AreaSelectionType MULTI_REGION = new AreaSelectionType("multi_region", "litematica.label.area_selection.selection_type.multi_region");

    public static final ImmutableList<AreaSelectionType> VALUES = ImmutableList.of(SIMPLE, MULTI_REGION);

    public AreaSelectionType(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
