package litematica.input;

import java.util.List;
import com.google.common.collect.ImmutableList;

import malilib.input.Hotkey;
import malilib.input.HotkeyCategory;
import malilib.input.HotkeyProvider;
import litematica.Reference;
import litematica.config.Configs;
import litematica.config.Hotkeys;

public class LitematicaHotkeyProvider implements HotkeyProvider
{
    public static final ImmutableList<Hotkey> ALL_HOTKEYS = buildFullHotkeyList();
    public static final LitematicaHotkeyProvider INSTANCE = new LitematicaHotkeyProvider();

    @Override
    public List<? extends Hotkey> getAllHotkeys()
    {
        return ALL_HOTKEYS;
    }

    @Override
    public List<HotkeyCategory> getHotkeysByCategories()
    {
        return ImmutableList.of(new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.hotkeys", Hotkeys.HOTKEY_LIST),
                                new HotkeyCategory(Reference.MOD_INFO, "litematica.hotkeys.category.generic", Configs.Generic.HOTKEYS),
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
