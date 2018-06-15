package fi.dy.masa.litematica.gui;

import org.lwjgl.input.Keyboard;
import com.mumfrey.liteloader.client.overlays.IGuiTextField;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;

public class GuiTextInput extends GuiLitematicaBase
{
    private final String title;
    private final GuiTextField textField;
    private final String originalText;
    private final IStringConsumer consumer;
    private int dialogWidth;
    private int dialogHeight;
    private int dialogLeft;
    private int dialogTop;

    public GuiTextInput(int maxTextLength, String titleKey, String text, GuiLitematicaBase parent, IStringConsumer consumer)
    {
        this.mc = Minecraft.getMinecraft();
        this.parent = parent;
        this.title = I18n.format(titleKey);
        this.originalText = text;
        this.consumer = consumer;

        this.dialogWidth = 260;
        this.dialogHeight = 100;

        if (this.parent != null)
        {
            this.dialogLeft = this.parent.width / 2 - this.dialogWidth / 2;
            this.dialogTop = this.parent.height / 2 - this.dialogHeight / 2;
        }
        else
        {
            ScaledResolution res = new ScaledResolution(this.mc);
            this.dialogLeft = res.getScaledWidth() / 2 - this.dialogWidth / 2;
            this.dialogTop = res.getScaledHeight() / 2 - this.dialogHeight / 2;
        }

        int width = Math.min(maxTextLength * 10, 240);
        this.textField = new GuiTextField(0, this.mc.fontRenderer, this.dialogLeft + 12, this.dialogTop + 40, width, 20);
        this.textField.setMaxStringLength(maxTextLength);
        this.textField.setFocused(true);
        this.textField.setText(this.originalText);
        this.textField.setCursorPositionEnd();
    }

    @Override
    protected String getTitle()
    {
        return this.title;
    }

    @Override
    public void initGui()
    {
        int x = this.dialogLeft + 10;
        int y = this.dialogTop + 70;
        int buttonWidth = 80;

        ButtonGeneric button;

        button = new ButtonGeneric(0, x, y, buttonWidth, 20, I18n.format("litematica.gui.button.ok"));
        this.addButton(button, this.createActionListener(ButtonListener.Type.OK));
        x += buttonWidth + 2;

        button = new ButtonGeneric(0, x, y, buttonWidth, 20, I18n.format("litematica.gui.button.reset"));
        this.addButton(button, this.createActionListener(ButtonListener.Type.RESET));
        x += buttonWidth + 2;

        button = new ButtonGeneric(0, x, y, buttonWidth, 20, I18n.format("litematica.gui.button.cancel"));
        this.addButton(button, this.createActionListener(ButtonListener.Type.CANCEL));
    }

    @Override
    public boolean doesGuiPauseGame()
    {
        return this.parent != null && this.parent.doesGuiPauseGame();
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        if (this.parent != null)
        {
            this.parent.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawOutlinedBox(this.dialogLeft, this.dialogTop, this.dialogWidth, this.dialogHeight, 0xB0000000, COLOR_HORIZONTAL_BAR);

        // Draw the title
        this.drawString(this.fontRenderer, this.getTitle(), this.dialogLeft + 10, this.dialogTop + 4, COLOR_WHITE);

        //super.drawScreen(mouseX, mouseY, partialTicks);
        this.textField.drawTextBox();

        this.drawButtons(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode)
    {
        if (keyCode == Keyboard.KEY_RETURN)
        {
            this.consumer.setString(this.textField.getText());
            this.mc.displayGuiScreen(this.parent);
            return true;
        }
        else if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.mc.displayGuiScreen(this.parent);
            return true;
        }

        if (this.textField.isFocused())
        {
            return this.textField.textboxKeyTyped(typedChar, keyCode);
        }

        return super.onKeyTyped(typedChar, keyCode);
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int button)
    {
        if (mouseX >= this.textField.x && mouseX < this.textField.x + ((IGuiTextField) this.textField).getInternalWidth() &&
            mouseY >= this.textField.y && mouseY < this.textField.y + ((IGuiTextField) this.textField).getHeight())
        {
            // Clear the field on right click
            if (button == 1)
            {
                this.textField.setText("");
            }
            else
            {
                this.textField.mouseClicked(mouseX, mouseY, button);
            }

            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, button);
    }

    private ButtonListener createActionListener(ButtonListener.Type type)
    {
        return new ButtonListener(type, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiTextInput gui;
        private final Type type;

        public ButtonListener(Type type, GuiTextInput gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.OK)
            {
                this.gui.consumer.setString(this.gui.textField.getText());
                this.gui.mc.displayGuiScreen(this.gui.parent);
            }
            else if (this.type == Type.CANCEL)
            {
                this.gui.mc.displayGuiScreen(this.gui.parent);
            }
            else if (this.type == Type.RESET)
            {
                this.gui.textField.setText(this.gui.originalText);
                this.gui.textField.setCursorPositionEnd();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            OK,
            CANCEL,
            RESET;
        }
    }
}
