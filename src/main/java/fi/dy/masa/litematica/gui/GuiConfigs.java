package fi.dy.masa.litematica.gui;

import java.util.Collections;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.config.gui.ConfigGuiTabBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiTab;

public class GuiConfigs extends GuiConfigsBase
{
    public static final ConfigGuiTabBase GENERIC       = new ConfigGuiTabBase("litematica.gui.button.config_gui.generic",        140, false, Configs.Generic.OPTIONS);
    public static final ConfigGuiTabBase INFO_OVERLAYS = new ConfigGuiTabBase("litematica.gui.button.config_gui.info_overlays",  140, false, Configs.InfoOverlays.OPTIONS);
    public static final ConfigGuiTabBase VISUALS       = new ConfigGuiTabBase("litematica.gui.button.config_gui.visuals",        140, false, Configs.Visuals.OPTIONS);
    public static final ConfigGuiTabBase COLORS        = new ConfigGuiTabBase("litematica.gui.button.config_gui.colors",         100, false, Configs.Colors.OPTIONS);
    public static final ConfigGuiTabBase HOTKEYS       = new ConfigGuiTabBase("litematica.gui.button.config_gui.hotkeys",        204, true,  Hotkeys.HOTKEY_LIST);
    public static final ConfigGuiTabBase RENDER_LAYERS = new ConfigGuiTabBase("litematica.gui.button.config_gui.render_layers",  204, false, Collections.emptyList(),
            (tab, gui) -> (button, mouseButton) -> { GuiBase.openGui(new GuiRenderLayer()); });

    public static final ImmutableList<IConfigGuiTab> TABS = ImmutableList.of(
            GENERIC,
            INFO_OVERLAYS,
            VISUALS,
            COLORS,
            HOTKEYS,
            RENDER_LAYERS
    );

    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, TABS, "litematica.gui.title.configs");
    }

    @Override
    public IConfigGuiTab getCurrentTab()
    {
        return DataManager.getConfigGuiTab();
    }

    @Override
    public void setCurrentTab(IConfigGuiTab tab)
    {
        DataManager.setConfigGuiTab(tab);
    }

    @Override
    public void initGui()
    {
        if (DataManager.getConfigGuiTab() == RENDER_LAYERS)
        {
            GuiBase.openGui(new GuiRenderLayer());
            return;
        }

        super.initGui();
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();

        SchematicWorldRefresher.INSTANCE.updateAll();
    }
}
