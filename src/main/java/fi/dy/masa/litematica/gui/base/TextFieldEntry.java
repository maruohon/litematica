package fi.dy.masa.litematica.gui.base;

import fi.dy.masa.litematica.gui.interfaces.ITextFieldListener;
import net.minecraft.client.gui.GuiTextField;

public class TextFieldEntry<T extends GuiTextField>
{
    private final T textField;
    private final ITextFieldListener<T> listener;
    
    public TextFieldEntry(T textField, ITextFieldListener<T> listener)
    {
        this.textField = textField;
        this.listener = listener;
    }

    public T getTextField()
    {
        return this.textField;
    }

    public ITextFieldListener<T> getListener()
    {
        return this.listener;
    }

    public void onGuiClosed()
    {
        if (this.listener != null)
        {
            this.listener.onGuiClosed(this.textField);
        }
    }

    public void draw()
    {
        this.textField.drawTextBox();
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.textField.mouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        return false;
    }

    public boolean keyTyped(char typedChar, int keyCode)
    {
        if (this.textField.textboxKeyTyped(typedChar, keyCode))
        {
            if (this.listener != null)
            {
                this.listener.onTextChange(this.textField);
            }

            return true;
        }

        return false;
    }
}
