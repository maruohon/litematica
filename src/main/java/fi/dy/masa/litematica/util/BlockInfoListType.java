package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;
import fi.dy.masa.malilib.config.value.OptionListConfigValue;
import fi.dy.masa.malilib.util.StringUtils;

public enum BlockInfoListType implements OptionListConfigValue<BlockInfoListType>
{
    ALL             ("all",             "litematica.gui.label.block_info_list_type.all"),
    RENDER_LAYERS   ("render_layers",   "litematica.gui.label.block_info_list_type.render_layers");

    public static final ImmutableList<BlockInfoListType> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    BlockInfoListType(String configString, String translationKey)
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
    public BlockInfoListType cycle(boolean forward)
    {
        return BaseOptionListConfigValue.cycleValue(VALUES, this.ordinal(), forward);
    }

    @Override
    public BlockInfoListType fromString(String name)
    {
        return BaseOptionListConfigValue.findValueByName(name, VALUES);
    }
}
