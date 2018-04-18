package fi.dy.masa.litematica.config.gui;

import com.mumfrey.liteloader.modconfig.ConfigPanelHost;
import fi.dy.masa.litematica.config.gui.button.ConfigButtonHotkey;

public abstract class ConfigPanelHotkeysBase extends ConfigPanelSub
{
    private ConfigButtonHotkey activeButton;

    public ConfigPanelHotkeysBase(String title, LitematicaConfigPanel parent)
    {
        super(title, parent);
    }

    public void setActiveButton(ConfigButtonHotkey button)
    {
        if (this.activeButton != null)
        {
            this.activeButton.onClearSelection();
        }

        this.activeButton = button;

        if (this.activeButton != null)
        {
            this.activeButton.onSelected();
        }
    }

    @Override
    public void keyPressed(ConfigPanelHost host, char keyChar, int keyCode)
    {
        if (this.activeButton != null)
        {
            this.activeButton.onKeyPressed(keyCode);
        }
        else
        {
            super.keyPressed(host, keyChar, keyCode);
        }
    }
}
