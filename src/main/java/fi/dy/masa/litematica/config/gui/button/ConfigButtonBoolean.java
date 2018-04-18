package fi.dy.masa.litematica.config.gui.button;

import fi.dy.masa.litematica.config.interfaces.IConfigBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public class ConfigButtonBoolean extends ConfigButtonBase
{
    private final IConfigBoolean config;

    public ConfigButtonBoolean(int id, int x, int y, int width, int height, IConfigBoolean config)
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
        this.config.setBooleanValue(! this.config.getBooleanValue());
        this.updateDisplayString();
        this.playPressSound(Minecraft.getMinecraft().getSoundHandler());
    }

    private void updateDisplayString()
    {
        String valueStr = String.valueOf(this.config.getBooleanValue());

        if (this.config.getBooleanValue())
        {
            this.displayString = TextFormatting.DARK_GREEN + valueStr + TextFormatting.RESET;
        }
        else
        {
            this.displayString = TextFormatting.DARK_RED + valueStr + TextFormatting.RESET;
        }
    }
}
