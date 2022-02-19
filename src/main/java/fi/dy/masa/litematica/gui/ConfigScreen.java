package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiScreen;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.gui.tab.BaseScreenTab;
import fi.dy.masa.malilib.gui.tab.ScreenTab;
import fi.dy.masa.malilib.gui.config.BaseConfigScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigTab;
import fi.dy.masa.malilib.gui.config.ConfigTab;
import fi.dy.masa.malilib.util.data.ModInfo;

public class ConfigScreen
{
    public static final ModInfo MOD_INFO = Reference.MOD_INFO;

    public static final BaseConfigTab GENERIC       = new BaseConfigTab(MOD_INFO, "generic",       160, Configs.Generic.OPTIONS,      ConfigScreen::create);
    public static final BaseConfigTab INFO_OVERLAYS = new BaseConfigTab(MOD_INFO, "info_overlays", 160, Configs.InfoOverlays.OPTIONS, ConfigScreen::create);
    public static final BaseConfigTab VISUALS       = new BaseConfigTab(MOD_INFO, "visuals",       160, Configs.Visuals.OPTIONS,      ConfigScreen::create);
    public static final BaseConfigTab COLORS        = new BaseConfigTab(MOD_INFO, "colors",        100, Configs.Colors.OPTIONS,       ConfigScreen::create);
    public static final BaseConfigTab HOTKEYS       = new BaseConfigTab(MOD_INFO, "hotkeys",       200, Hotkeys.HOTKEY_LIST,          ConfigScreen::create);
    public static final BaseScreenTab RENDER_LAYERS = new BaseScreenTab("litematica.label.config_tab.render_layers", (scr) -> scr instanceof GuiRenderLayer, ConfigScreen::openRenderLayersScreen);

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

    public static ImmutableList<ConfigTab> getConfigTabs()
    {
        return CONFIG_TABS;
    }

    public static BaseConfigScreen create()
    {
        BaseConfigScreen screen = new BaseConfigScreen(MOD_INFO, null, ALL_TABS, VISUALS, "litematica.gui.title.configs");
        screen.setConfigSaveListener(SchematicWorldRenderingNotifier.INSTANCE::updateAll);
        return screen;
    }

    public static BaseConfigScreen create(@Nullable GuiScreen currentScreen)
    {
        BaseConfigScreen screen = new BaseConfigScreen(MOD_INFO, null, ALL_TABS, VISUALS, "litematica.gui.title.configs");
        screen.setConfigSaveListener(SchematicWorldRenderingNotifier.INSTANCE::updateAll);
        return screen;
    }

    public static BaseConfigScreen createOnTab(ConfigTab tab)
    {
        BaseConfigScreen screen = new BaseConfigScreen(MOD_INFO, null, ALL_TABS, VISUALS, "litematica.gui.title.configs");
        screen.setCurrentTab(tab);
        DataManager.setConfigGuiTab(tab);
        return screen;
    }

    public static GuiRenderLayer openRenderLayersScreen(@Nullable GuiScreen currentScreen)
    {
        GuiRenderLayer screen = new GuiRenderLayer();
        //screen.setCurrentTab(RENDER_LAYERS);
        return screen;
    }
}
