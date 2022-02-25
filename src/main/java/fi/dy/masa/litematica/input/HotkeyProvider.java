package fi.dy.masa.litematica.input;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.input.Hotkey;
import fi.dy.masa.malilib.input.HotkeyCategory;

public class HotkeyProvider implements fi.dy.masa.malilib.input.HotkeyProvider
{
    public static final ImmutableList<Hotkey> ALL_HOTKEYS = buildFullHotkeyList();

    @Override
    public List<? extends Hotkey> getAllHotkeys()
    {
        return ALL_HOTKEYS;
    }

    @Override
    public List<HotkeyCategory> getHotkeysByCategories()
    {
        return ImmutableList.of(new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.hotkeys",  Hotkeys.HOTKEY_LIST),
                                new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.generic",  Configs.Generic.HOTKEYS),
                                new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.visuals",  Configs.Visuals.HOTKEYS),
                                new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.overlays", Configs.InfoOverlays.HOTKEYS));
    }

    private static ImmutableList<Hotkey> buildFullHotkeyList()
    {
        ImmutableList.Builder<Hotkey> builder = ImmutableList.builder();

        builder.addAll(Hotkeys.HOTKEY_LIST);
        builder.addAll(Configs.Generic.HOTKEYS);
        builder.addAll(Configs.Visuals.HOTKEYS);
        builder.addAll(Configs.InfoOverlays.HOTKEYS);

        return builder.build();
    }
}
