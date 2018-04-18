package fi.dy.masa.litematica.config.gui.button;

import net.minecraft.client.gui.GuiButton;

public abstract class ConfigButtonBase extends GuiButton
{
    public ConfigButtonBase(int id, int x, int y, int width, int height)
    {
        super(id, x, y, width, height, "");
    }

    public abstract void onMouseClicked();

    public abstract void onMouseButtonClicked(int mouseButton);
}
