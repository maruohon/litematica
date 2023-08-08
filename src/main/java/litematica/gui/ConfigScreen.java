package litematica.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.google.common.collect.ImmutableList;

import malilib.config.group.ExpandableConfigGroup;
import malilib.config.option.ConfigInfo;
import malilib.config.util.ConfigUtils;
import malilib.gui.BaseScreen;
import malilib.gui.config.BaseConfigScreen;
import malilib.gui.config.BaseConfigTab;
import malilib.gui.config.ConfigTab;
import malilib.gui.tab.BaseScreenTab;
import malilib.gui.tab.ScreenTab;
import malilib.util.data.ModInfo;
import litematica.Reference;
import litematica.config.Configs;
import litematica.config.Hotkeys;
import litematica.data.DataManager;
import litematica.world.SchematicWorldRenderingNotifier;

public class ConfigScreen
{
    public static final ModInfo MOD_INFO = Reference.MOD_INFO;

    public static final BaseConfigTab GENERIC       = new BaseConfigTab(MOD_INFO, "generic",       160, getGenericConfigs(),            ConfigScreen::create);
    public static final BaseConfigTab INFO_OVERLAYS = new BaseConfigTab(MOD_INFO, "info_overlays", 160, Configs.InfoOverlays.OPTIONS,   ConfigScreen::create);
    public static final BaseConfigTab VISUALS       = new BaseConfigTab(MOD_INFO, "visuals",       160, Configs.Visuals.OPTIONS,        ConfigScreen::create);
    public static final BaseConfigTab COLORS        = new BaseConfigTab(MOD_INFO, "colors",        100, Configs.Colors.OPTIONS,         ConfigScreen::create);
    public static final BaseConfigTab HOTKEYS       = new BaseConfigTab(MOD_INFO, "hotkeys",       200, getHotkeys(),                   ConfigScreen::create);
    public static final BaseScreenTab RENDER_LAYERS = new BaseScreenTab(MOD_INFO, "render_layers", RenderLayerEditScreen::screenValidator, RenderLayerEditScreen::openRenderLayerEditScreen);

    public static final ImmutableList<ConfigTab> CONFIG_TABS = ImmutableList.of(
            GENERIC,
            INFO_OVERLAYS,
            VISUALS,
            COLORS,
            HOTKEYS
    );

    public static final ImmutableList<ScreenTab> ALL_TABS = ImmutableList.of(
            GENERIC,
            INFO_OVERLAYS,
            VISUALS,
            COLORS,
            HOTKEYS,
            RENDER_LAYERS
    );

    private static final HashMap<ScreenTab, List<ConfigInfo>> EXTENSION_MOD_CONFIGS = new HashMap<>();

    /**
     * If you are an extension mod, you can use this method to add your custom configs
     * to be shown on the main mod config screens.
     */
    public static void addExtensionModHotkeys(BaseConfigTab tab, List<? extends ConfigInfo> configs)
    {
        // TODO
        List<ConfigInfo> list = EXTENSION_MOD_CONFIGS.computeIfAbsent(tab, (t) -> new ArrayList<>());
        list.addAll(configs);
    }

    public static ImmutableList<ConfigTab> getConfigTabs()
    {
        return CONFIG_TABS;
    }

    public static BaseConfigScreen create()
    {
        BaseConfigScreen screen = BaseConfigScreen.withExtensionModTabs(MOD_INFO, ALL_TABS, GENERIC,
                                                                        "litematica.title.screen.configs",
                                                                        Reference.MOD_VERSION);
        screen.setConfigSaveListener(SchematicWorldRenderingNotifier.INSTANCE::updateAll);
        return screen;
    }

    public static BaseConfigScreen createOnTab(ConfigTab tab)
    {
        BaseConfigScreen screen = create();
        screen.setCurrentTab(tab);
        DataManager.setConfigGuiTab(tab);
        return screen;
    }

    public static void openConfigScreen()
    {
        BaseScreen.openScreen(create());
    }

    private static ImmutableList<ConfigInfo> getGenericConfigs()
    {
        ArrayList<ConfigInfo> genericConfigs = new ArrayList<>(Configs.Generic.OPTIONS);
        ArrayList<ConfigInfo> list = new ArrayList<>(Configs.Nags.OPTIONS);

        ConfigUtils.sortConfigsInPlaceByDisplayName(list);
        genericConfigs.add(new ExpandableConfigGroup(MOD_INFO, "nags", list));
        ConfigUtils.sortConfigsInPlaceByDisplayName(genericConfigs);

        return ImmutableList.copyOf(genericConfigs);
    }

    private static ImmutableList<ConfigInfo> getHotkeys()
    {
        ArrayList<ConfigInfo> list = new ArrayList<>(Hotkeys.HOTKEY_LIST);

        list.add(ConfigUtils.extractOptionsToExpandableGroup(list, MOD_INFO, "hotkey.open_screen",      c -> c.getName().startsWith("open")));
        list.add(ConfigUtils.extractOptionsToExpandableGroup(list, MOD_INFO, "hotkey.render_layer",     c -> c.getName().startsWith("layer")));
        list.add(ConfigUtils.extractOptionsToExpandableGroup(list, MOD_INFO, "hotkey.schematic_edit",   c -> c.getName().startsWith("schematicEdit")));
        list.add(ConfigUtils.extractOptionsToExpandableGroup(list, MOD_INFO, "hotkey.selection",        c -> c.getName().startsWith("selection")));
        list.add(ConfigUtils.extractOptionsToExpandableGroup(list, MOD_INFO, "hotkey.tool",             c -> c.getName().startsWith("tool")));

        ConfigUtils.sortConfigsInPlaceByDisplayName(list);

        return ImmutableList.copyOf(list);
    }
}
