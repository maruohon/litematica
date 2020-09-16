package fi.dy.masa.litematica.gui;

import java.util.Collections;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigTab;
import fi.dy.masa.malilib.gui.config.ConfigTab;

public class ConfigScreen
{
    public static final BaseConfigTab GENERIC       = new BaseConfigTab("litematica.gui.button.config_gui.generic",         Reference.MOD_NAME, 160, Configs.Generic.OPTIONS);
    public static final BaseConfigTab INFO_OVERLAYS = new BaseConfigTab("litematica.gui.button.config_gui.info_overlays",   Reference.MOD_NAME, 160, Configs.InfoOverlays.OPTIONS);
    public static final BaseConfigTab VISUALS       = new BaseConfigTab("litematica.gui.button.config_gui.visuals",         Reference.MOD_NAME, 160, Configs.Visuals.OPTIONS);
    public static final BaseConfigTab COLORS        = new BaseConfigTab("litematica.gui.button.config_gui.colors",          Reference.MOD_NAME, 100, Configs.Colors.OPTIONS);
    public static final BaseConfigTab HOTKEYS       = new BaseConfigTab("litematica.gui.button.config_gui.hotkeys",         Reference.MOD_NAME, 200, Hotkeys.HOTKEY_LIST);
    public static final BaseConfigTab RENDER_LAYERS = new BaseConfigTab("litematica.gui.button.config_gui.render_layers",   Reference.MOD_NAME, 200, Collections.emptyList(),
                                                                        (tab, gui) -> (button, mouseButton) -> openRenderLayers(gui));

    public static final ImmutableList<ConfigTab> TABS = ImmutableList.of(
            GENERIC,
            INFO_OVERLAYS,
            VISUALS,
            COLORS,
            HOTKEYS,
            RENDER_LAYERS
    );

    public static BaseConfigScreen create()
    {
        BaseConfigScreen screen = new BaseConfigScreen(Reference.MOD_ID, null, TABS, VISUALS, "litematica.gui.title.configs");
        screen.setConfigSaveListener(SchematicWorldRenderingNotifier.INSTANCE::updateAll);
        return screen;
    }

    public static BaseConfigScreen createOnTab(ConfigTab tab)
    {
        BaseConfigScreen screen = new BaseConfigScreen(Reference.MOD_ID, null, TABS, VISUALS, "litematica.gui.title.configs");
        screen.setCurrentTab(tab);
        return screen;
    }

    public static ImmutableList<ConfigTab> getConfigTabs()
    {
        return TABS;
    }

    private static boolean openRenderLayers(BaseConfigScreen screen)
    {
        screen.setCurrentTab(RENDER_LAYERS);
        BaseScreen.openGui(new GuiRenderLayer());
        return true;
    }
}
