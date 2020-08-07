package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseConfigOptionListEntry;
import fi.dy.masa.malilib.config.value.ConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

public enum BlockInfoAlignment implements ConfigOptionListEntry<BlockInfoAlignment>
{
    CENTER      ("center",      "litematica.label.alignment.center"),
    TOP_CENTER  ("top_center",  "litematica.label.alignment.top_center");

    public static final ImmutableList<BlockInfoAlignment> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    BlockInfoAlignment(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public BlockInfoAlignment cycle(boolean forward)
    {
        return BaseConfigOptionListEntry.cycleValue(VALUES, this.ordinal(), forward);
    }

    @Override
    public BlockInfoAlignment fromString(String name)
    {
        return BaseConfigOptionListEntry.findValueByName(name, VALUES);
    }
}
