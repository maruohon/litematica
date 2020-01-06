package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;

public abstract class GuiSchematicSaveBase extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected GuiTextFieldGeneric textField;
    protected WidgetCheckBox checkboxIgnoreEntities;
    protected String lastText = "";
    protected String defaultText = "";
    @Nullable protected final ISchematic schematic;

    public GuiSchematicSaveBase(@Nullable ISchematic schematic)
    {
        super(10, 70);

        this.schematic = schematic;

        this.textField = new GuiTextFieldGeneric(10, 32, 160, 20, this.textRenderer);
        this.textField.setMaxStringLength(256);
        this.textField.setFocused(true);
    }

    @Override
    public int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    public int getMaxInfoHeight()
    {
        return super.getMaxInfoHeight();
    }

    @Override
    public void initGui()
    {
        super.initGui();

        boolean focused = this.textField.isFocused();
        String text = this.textField.getText();
        int pos = this.textField.getCursorPosition();
        this.textField = new GuiTextFieldGeneric(10, 32, this.width - 196, 20, this.textRenderer);
        this.textField.setText(text);
        this.textField.setCursorPosition(pos);
        this.textField.setFocused(focused);

        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        // Only set the text field contents if it hasn't been set already.
        // This prevents overwriting any user input text when switching to a newly created directory.
        if (this.lastText.isEmpty())
        {
            if (entry != null && entry.getType() != DirectoryEntryType.DIRECTORY && entry.getType() != DirectoryEntryType.INVALID)
            {
                this.setTextFieldText(FileUtils.getNameWithoutExtension(entry.getName()));
            }
            else if (this.schematic != null)
            {
                this.setTextFieldText(this.schematic.getMetadata().getName());
            }
            else
            {
                this.setTextFieldText(this.defaultText);
            }
        }

        int x = this.textField.x + this.textField.getWidth() + 12;
        int y = 32;

        String str = StringUtils.translate("litematica.gui.label.schematic_save.checkbox.ignore_entities");
        this.checkboxIgnoreEntities = new WidgetCheckBox(x, y + 24, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, str);
        this.addWidget(this.checkboxIgnoreEntities);

        x = this.createButton(x, y, ButtonType.SAVE);
    }

    protected void setTextFieldText(String text)
    {
        this.lastText = text;
        this.textField.setText(text);
        this.textField.setCursorPositionEnd();
    }

    protected String getTextFieldText()
    {
        return this.textField.getText();
    }

    protected abstract void saveSchematic();

    private int createButton(int x, int y, ButtonType type)
    {
        String label = StringUtils.translate(type.getLabelKey());
        int width = this.getStringWidth(label) + 10;

        ButtonGeneric button;

        if (type == ButtonType.SAVE)
        {
            button = new ButtonGeneric(x, y, width, 20, label, "litematica.gui.label.schematic_save.hoverinfo.hold_shift_to_overwrite");
        }
        else
        {
            button = new ButtonGeneric(x, y, width, 20, label);
        }

        this.addButton(button, new ButtonListenerSave(this));

        return x + width + 4;
    }

    @Override
    public void setString(String string)
    {
        this.setNextMessageType(MessageType.ERROR);
        super.setString(string);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(mouseX, mouseY, partialTicks);

        this.textField.drawTextBox();
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null && entry.getType() != DirectoryEntryType.DIRECTORY && entry.getType() != DirectoryEntryType.INVALID)
        {
            this.setTextFieldText(FileUtils.getNameWithoutExtension(entry.getName()));
        }
    }

    @Override
    protected ISelectionListener<DirectoryEntry> getSelectionListener()
    {
        return this;
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.textField.mouseClicked(mouseX, mouseY, mouseButton))
        {
            return true;
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(char typedChar, int keyCode)
    {
        if (this.textField.isFocused() && keyCode == Keyboard.KEY_RETURN)
        {
            this.saveSchematic();
            return true;
        }
        else if (this.textField.textboxKeyTyped(typedChar, keyCode))
        {
            this.getListWidget().clearSelection();
            return true;
        }
        else if (keyCode == Keyboard.KEY_TAB)
        {
            this.textField.setFocused(! this.textField.isFocused());
            return true;
        }

        return super.onKeyTyped(typedChar, keyCode);
    }

    private static class ButtonListenerSave implements IButtonActionListener
    {
        protected final GuiSchematicSaveBase gui;

        public ButtonListenerSave(GuiSchematicSaveBase gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            this.gui.saveSchematic();
        }
    }

    public enum ButtonType
    {
        SAVE ("litematica.gui.button.save_schematic");

        private final String labelKey;

        private ButtonType(String labelKey)
        {
            this.labelKey = labelKey;
        }

        public String getLabelKey()
        {
            return this.labelKey;
        }
    }
}
