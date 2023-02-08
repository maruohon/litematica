package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.config.value.BaseOptionListConfigValue;

public class ReplaceBehavior extends BaseOptionListConfigValue
{
    public static final ReplaceBehavior NONE         = new ReplaceBehavior("none",         "litematica.gui.label.replace_behavior.none");
    public static final ReplaceBehavior ALL          = new ReplaceBehavior("all",          "litematica.gui.label.replace_behavior.all");
    public static final ReplaceBehavior WITH_NON_AIR = new ReplaceBehavior("with_non_air", "litematica.gui.label.replace_behavior.with_non_air");

    public static final ImmutableList<ReplaceBehavior> VALUES = ImmutableList.of(NONE, ALL, WITH_NON_AIR);

    public ReplaceBehavior(String name, String translationKey)
    {
        super(name, translationKey);
    }
}
