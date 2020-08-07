package fi.dy.masa.litematica.gui;

import java.util.Collections;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;
import fi.dy.masa.malilib.gui.config.BaseConfigTab;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.config.BaseConfigScreen;
import fi.dy.masa.malilib.gui.config.ConfigTab;

public class ConfigScreen extends BaseConfigScreen
{
    public static final BaseConfigTab GENERIC       = new BaseConfigTab("litematica.gui.button.config_gui.generic", 140, false, Configs.Generic.OPTIONS);
    public static final BaseConfigTab INFO_OVERLAYS = new BaseConfigTab("litematica.gui.button.config_gui.info_overlays", 140, false, Configs.InfoOverlays.OPTIONS);
    public static final BaseConfigTab VISUALS       = new BaseConfigTab("litematica.gui.button.config_gui.visuals", 140, false, Configs.Visuals.OPTIONS);
    public static final BaseConfigTab COLORS        = new BaseConfigTab("litematica.gui.button.config_gui.colors", 100, false, Configs.Colors.OPTIONS);
    public static final BaseConfigTab HOTKEYS       = new BaseConfigTab("litematica.gui.button.config_gui.hotkeys", 204, true, Hotkeys.HOTKEY_LIST);
    public static final BaseConfigTab RENDER_LAYERS = new BaseConfigTab("litematica.gui.button.config_gui.render_layers", 204, false, Collections.emptyList(),
                                                                        (tab, gui) -> (button, mouseButton) -> { BaseScreen.openGui(new GuiRenderLayer()); });

    public static final ImmutableList<ConfigTab> TABS = ImmutableList.of(
            GENERIC,
            INFO_OVERLAYS,
            VISUALS,
            COLORS,
            HOTKEYS,
            RENDER_LAYERS
    );

    public ConfigScreen()
    {
        super(10, 50, Reference.MOD_ID, null, TABS, "litematica.gui.title.configs");
    }

    @Override
    public ConfigTab getCurrentTab()
    {
        return DataManager.getConfigGuiTab();
    }

    @Override
    public void setCurrentTab(ConfigTab tab)
    {
        DataManager.setConfigGuiTab(tab);
    }

    @Override
    public void initGui()
    {
        if (DataManager.getConfigGuiTab() == RENDER_LAYERS)
        {
            BaseScreen.openGui(new GuiRenderLayer());
            return;
        }

        super.initGui();
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();

        SchematicWorldRenderingNotifier.INSTANCE.updateAll();
    }
}
