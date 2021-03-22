package fi.dy.masa.litematica.selection;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;
import fi.dy.masa.malilib.config.value.OptionListConfigValue;
import fi.dy.masa.malilib.util.StringUtils;

public enum CornerSelectionMode implements OptionListConfigValue<CornerSelectionMode>
{
    EXPAND      ("expand",      "litematica.hud.area_selection.mode.expand"),
    CORNERS     ("corners",     "litematica.hud.area_selection.mode.corners");

    public static final ImmutableList<CornerSelectionMode> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    CornerSelectionMode(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getName()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public CornerSelectionMode cycle(boolean forward)
    {
        return BaseOptionListConfigValue.cycleValue(VALUES, this.ordinal(), forward);
    }

    @Override
    public CornerSelectionMode fromString(String name)
    {
        return BaseOptionListConfigValue.findValueByName(name, VALUES);
    }
}
