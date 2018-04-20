package fi.dy.masa.litematica.config.gui.button;

import com.mumfrey.liteloader.modconfig.AbstractConfigPanel.ConfigOptionListener;
import net.minecraft.client.Minecraft;

public class ButtonEntry<T extends ButtonBase>
{
    private final T button;
    private final IButtonActionListener<T> listener;
    
    public ButtonEntry(T button, IButtonActionListener<T> listener)
    {
        this.button = button;
        this.listener = listener;
    }

    public T getButton()
    {
        return this.button;
    }

    public ConfigOptionListener<T> getListener()
    {
        return this.listener;
    }

    public void draw(Minecraft minecraft, int mouseX, int mouseY, float partialTicks)
    {
        this.button.drawButton(minecraft, mouseX, mouseY, partialTicks);
    }

    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY, int mouseButton)
    {
        if (this.button.mousePressed(minecraft, mouseX, mouseY))
        {
            this.button.onMouseButtonClicked(mouseButton);
            this.button.playPressSound(minecraft.getSoundHandler());

            if (this.listener != null)
            {
                this.listener.actionPerformedWithButton(this.button, mouseButton);
            }

            return true;
        }

        return false;
    }

    public void mouseReleased(Minecraft minecraft, int mouseX, int mouseY)
    {
        this.button.mouseReleased(mouseX, mouseY);
    }
}
