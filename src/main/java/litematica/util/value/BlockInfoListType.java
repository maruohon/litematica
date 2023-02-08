package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionListConfigValue;

public class BlockInfoListType extends BaseOptionListConfigValue
{
    public static final BlockInfoListType ALL           = new BlockInfoListType("all",             "litematica.gui.label.block_info_list_type.all");
    public static final BlockInfoListType RENDER_LAYERS = new BlockInfoListType("render_layers",   "litematica.gui.label.block_info_list_type.render_layers");

    public static final ImmutableList<BlockInfoListType> VALUES = ImmutableList.of(ALL, RENDER_LAYERS);

    public BlockInfoListType(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
