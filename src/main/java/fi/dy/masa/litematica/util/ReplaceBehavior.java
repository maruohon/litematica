package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;
import fi.dy.masa.malilib.config.value.OptionListConfigValue;
import fi.dy.masa.malilib.util.StringUtils;

public enum ReplaceBehavior implements OptionListConfigValue<ReplaceBehavior>
{
    NONE            ("none",            "litematica.gui.label.replace_behavior.none"),
    ALL             ("all",             "litematica.gui.label.replace_behavior.all"),
    WITH_NON_AIR    ("with_non_air",    "litematica.gui.label.replace_behavior.with_non_air");

    public static final ImmutableList<ReplaceBehavior> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    ReplaceBehavior(String configString, String translationKey)
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
    public ReplaceBehavior cycle(boolean forward)
    {
        return BaseOptionListConfigValue.cycleValue(VALUES, this.ordinal(), forward);
    }

    @Override
    public ReplaceBehavior fromString(String name)
    {
        return BaseOptionListConfigValue.findValueByName(name, VALUES);
    }
}
