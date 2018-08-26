package fi.dy.masa.litematica.gui;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.malilib.config.ConfigType;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigHotkey;
import fi.dy.masa.malilib.config.IConfigOptionList;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ConfigButtonBoolean;
import fi.dy.masa.malilib.gui.button.ConfigButtonKeybind;
import fi.dy.masa.malilib.gui.button.ConfigButtonOptionList;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.minecraft.client.resources.I18n;

public class GuiConfigs extends GuiConfigsBase
{
    private int id;

    public GuiConfigs()
    {
        super(null);
    }

    @Override
    public void initGui()
    {
        this.clearOptions();

        this.id = 0;
        int x = 10;
        int y = 26;
        x += this.createButton(x, y, 80, ConfigGuiTab.GENERIC) + 4;
        x += this.createButton(x, y, 80, ConfigGuiTab.VISUALS) + 4;
        x += this.createButton(x, y, 80, ConfigGuiTab.COLORS) + 4;

        this.addOptions(this.getConfigs());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();

        String title = I18n.format("litematica.gui.title.configs");
        this.drawString(this.mc.fontRenderer, title, 10, 10, 0xFFFFFFFF);

        this.drawGui(mouseX, mouseY, partialTicks);
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonListener listener = new ButtonListener(tab, this);
        boolean enabled = DataManager.getConfigGuiTab() != tab;
        String label = tab.getDisplayName();

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        button.enabled = enabled;
        this.addButton(button, listener);

        return width;
    }

    @Override
    protected Collection<IConfigValue> getConfigs()
    {
        ConfigGuiTab tab = DataManager.getConfigGuiTab();

        if (tab == ConfigGuiTab.GENERIC)
        {
            return Configs.Generic.OPTIONS;
        }
        else if (tab == ConfigGuiTab.VISUALS)
        {
            return Configs.Visuals.OPTIONS;
        }
        else if (tab == ConfigGuiTab.COLORS)
        {
            return Configs.Colors.OPTIONS;
        }

        return Collections.emptyList();
    }

    protected void addOptions(Collection<IConfigValue> configs)
    {
        final int xStart = 10;
        int x = xStart;
        int y = 50;
        int configWidth = this.elementWidth;
        int configHeight = 20;
        int labelWidth = this.getMaxLabelWidth(configs);
        int id = 0;

        for (IConfigValue config : configs)
        {
            this.addLabel(id++, x, y + 7, labelWidth, 8, 0xFFFFFFFF, config.getName());

            String comment = config.getComment();
            ConfigType type = config.getType();

            if (comment != null)
            {
                this.addConfigComment(x, y + 2, labelWidth, 10, comment);
            }

            x += labelWidth + 10;

            if (type == ConfigType.BOOLEAN && (config instanceof IConfigBoolean))
            {
                ConfigButtonBoolean optionButton = new ConfigButtonBoolean(id++, x, y, configWidth, configHeight, (IConfigBoolean) config);
                this.addConfigButtonEntry(id++, x + configWidth + 10, y, config, optionButton);
            }
            else if (type == ConfigType.OPTION_LIST && (config instanceof IConfigOptionList))
            {
                ConfigButtonOptionList optionButton = new ConfigButtonOptionList(id++, x, y, configWidth, configHeight, (IConfigOptionList) config);
                this.addConfigButtonEntry(id++, x + configWidth + 10, y, config, optionButton);
            }
            else if (type == ConfigType.HOTKEY && (config instanceof IConfigHotkey))
            {
                IKeybind keybind = ((IConfigHotkey) config).getKeybind();
                ConfigButtonKeybind keybindButton = new ConfigButtonKeybind(id++, x, y, configWidth, configHeight, keybind, this);

                this.addButton(keybindButton, this.getConfigListener());
                this.addKeybindResetButton(id++, x + configWidth + 10, y, keybind, keybindButton);
            }
            else if (type == ConfigType.STRING ||
                     type == ConfigType.COLOR ||
                     type == ConfigType.INTEGER ||
                     type == ConfigType.DOUBLE)
            {
                this.addConfigTextFieldEntry(id++, x, y, configWidth, configHeight, config);
            }

            x = xStart;
            y += configHeight + 1;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (this.keyPressed(typedChar, keyCode) == false)
        {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        if (this.mousePressed(mouseX, mouseY, mouseButton) == false)
        {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiConfigs parent;
        private final ConfigGuiTab tab;

        public ButtonListener(ConfigGuiTab tab, GuiConfigs parent)
        {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            DataManager.setConfigGuiTab(this.tab);
            this.parent.initGui();
        }
    }

    public enum ConfigGuiTab
    {
        GENERIC ("litematica.gui.button.config_gui.generic"),
        VISUALS ("litematica.gui.button.config_gui.visuals"),
        COLORS  ("litematica.gui.button.config_gui.colors");

        private final String translationKey;

        private ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return I18n.format(this.translationKey);
        }
    }
}
