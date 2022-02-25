package fi.dy.masa.litematica.util;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.value.BaseOptionListConfigValue;
import fi.dy.masa.malilib.config.value.VerticalAlignment;

public class BlockInfoAlignment extends BaseOptionListConfigValue
{
    public static final BlockInfoAlignment CENTER     = new BlockInfoAlignment("center",      "litematica.label.alignment.center",      VerticalAlignment.CENTER);
    public static final BlockInfoAlignment TOP_CENTER = new BlockInfoAlignment("top_center",  "litematica.label.alignment.top_center",  VerticalAlignment.TOP);

    public static final ImmutableList<BlockInfoAlignment> VALUES = ImmutableList.of(TOP_CENTER, CENTER);

    protected final VerticalAlignment verticalAlign;

    public BlockInfoAlignment(String name, String translationKey, VerticalAlignment verticalAlign)
    {
        super(name, translationKey);

        this.verticalAlign = verticalAlign;
    }

    public VerticalAlignment getVerticalAlign()
    {
        return this.verticalAlign;
    }
}
