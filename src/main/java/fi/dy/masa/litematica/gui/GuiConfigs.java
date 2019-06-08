package fi.dy.masa.litematica.gui;

import java.util.Collections;
import java.util.List;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiConfigs extends GuiConfigsBase
{
    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, "litematica.gui.title.configs");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();

        if (DataManager.getConfigGuiTab() == ConfigGuiTab.RENDER_LAYERS)
        {
            GuiBase.openGui(new GuiRenderLayer());
            return;
        }

        int x = 10;
        int y = 26;

        x += this.createButton(x, y, -1, ConfigGuiTab.GENERIC);
        x += this.createButton(x, y, -1, ConfigGuiTab.INFO_OVERLAYS);
        x += this.createButton(x, y, -1, ConfigGuiTab.VISUALS);
        x += this.createButton(x, y, -1, ConfigGuiTab.COLORS);
        x += this.createButton(x, y, -1, ConfigGuiTab.HOTKEYS);
        x += this.createButton(x, y, -1, ConfigGuiTab.RENDER_LAYERS);
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(DataManager.getConfigGuiTab() != tab);
        this.addButton(button, new ButtonListener(tab, this));

        return button.getWidth() + 2;
    }

    @Override
    protected int getConfigWidth()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC || tab == ConfigGuiTab.INFO_OVERLAYS || tab == ConfigGuiTab.VISUALS)
        {
            return 140;
        }
        if (tab == ConfigGuiTab.COLORS)
        {
            return 100;
        }

        return super.getConfigWidth();
    }

    @Override
    protected boolean useKeybindSearch()
    {
        return DataManager.getConfigGuiTab() == ConfigGuiTab.HOTKEYS;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs;
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC)
        {
            configs = Configs.Generic.OPTIONS;
        }
        else if (tab == ConfigGuiTab.INFO_OVERLAYS)
        {
            configs = Configs.InfoOverlays.OPTIONS;
        }
        else if (tab == ConfigGuiTab.VISUALS)
        {
            configs = Configs.Visuals.OPTIONS;
        }
        else if (tab == ConfigGuiTab.COLORS)
        {
            configs = Configs.Colors.OPTIONS;
        }
        else if (tab == ConfigGuiTab.HOTKEYS)
        {
            configs = Hotkeys.HOTKEY_LIST;
        }
        else
        {
            return Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    protected void onSettingsChanged()
    {
        super.onSettingsChanged();

        SchematicWorldRefresher.INSTANCE.updateAll();
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final GuiConfigs parent;
        private final ConfigGuiTab tab;

        public ButtonListener(ConfigGuiTab tab, GuiConfigs parent)
        {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);

            if (this.tab != ConfigGuiTab.RENDER_LAYERS)
            {
                this.parent.reCreateListWidget(); // apply the new config width
                this.parent.getListWidget().resetScrollbarPosition();
                this.parent.initGui();
            }
            else
            {
                GuiBase.openGui(new GuiRenderLayer());
            }
        }
    }

    public enum ConfigGuiTab
    {
        GENERIC         ("litematica.gui.button.config_gui.generic"),
        INFO_OVERLAYS   ("litematica.gui.button.config_gui.info_overlays"),
        VISUALS         ("litematica.gui.button.config_gui.visuals"),
        COLORS          ("litematica.gui.button.config_gui.colors"),
        HOTKEYS         ("litematica.gui.button.config_gui.hotkeys"),
        RENDER_LAYERS   ("litematica.gui.button.config_gui.render_layers");

        private final String translationKey;

        private ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }
}
