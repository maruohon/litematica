package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;

public class BlockInfoAlignment extends BaseOptionListConfigValue
{
    public static final BlockInfoAlignment CENTER     = new BlockInfoAlignment("center",      "litematica.label.alignment.center");
    public static final BlockInfoAlignment TOP_CENTER = new BlockInfoAlignment("top_center",  "litematica.label.alignment.top_center");

    public static final ImmutableList<BlockInfoAlignment> VALUES = ImmutableList.of(TOP_CENTER, CENTER);

    public BlockInfoAlignment(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
