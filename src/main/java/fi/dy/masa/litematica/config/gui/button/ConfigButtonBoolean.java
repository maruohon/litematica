package fi.dy.masa.litematica.config.gui.button;

import fi.dy.masa.litematica.config.options.ConfigBoolean;
import fi.dy.masa.litematica.gui.button.ButtonBase;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public class ConfigButtonBoolean extends ButtonBase
{
    private final ConfigBoolean config;

    public ConfigButtonBoolean(int id, int x, int y, int width, int height, ConfigBoolean config)
    {
        super(id, x, y, width, height);
        this.config = config;

        this.updateDisplayString();
    }

    @Override
    public void onMouseClicked()
    {
    }

    @Override
    public void onMouseButtonClicked(int mouseButton)
    {
        this.config.setValue(! this.config.getValue());
        this.updateDisplayString();
        this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
    }

    private void updateDisplayString()
    {
        String valueStr = String.valueOf(this.config.getValue());

        if (this.config.getValue())
        {
            this.displayString = TextFormatting.DARK_GREEN + valueStr + TextFormatting.RESET;
        }
        else
        {
            this.displayString = TextFormatting.DARK_RED + valueStr + TextFormatting.RESET;
        }
    }
}
