package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.gui.DrawContext;

import javax.annotation.Nullable;

public abstract class GuiSchematicSaveBase extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected GuiTextFieldGeneric textField;
    protected WidgetCheckBox checkboxIgnoreEntities;
    protected WidgetCheckBox checkboxVisibleOnly;
    protected final WidgetCheckBox checkboxSaveFromSchematicWorld;
    protected String lastText = "";
    protected String defaultText = "";
    @Nullable protected final LitematicaSchematic schematic;

    public GuiSchematicSaveBase(@Nullable LitematicaSchematic schematic)
    {
        super(10, 70);

        this.schematic = schematic;

        this.textField = new GuiTextFieldGeneric(10, 32, 160, 20, this.textRenderer);
        this.textField.setMaxLength(256);
        this.textField.setFocused(true);

        this.checkboxSaveFromSchematicWorld = new WidgetCheckBox(0, 0, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, "Save from schematic world", "If enabled, then the schematic is created by saving the\ncontents of the selection from the schematic world\ninstead of the normal vanilla world.\nThis allows you to combine or trim schematics without having\nto paste them to a temporary creative world.");
    }

    @Override
    public int getBrowserHeight()
    {
        return this.height - 80;
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

        int x = this.textField.getX() + this.textField.getWidth() + 12;
        int y = 32;

        String str = StringUtils.translate("litematica.gui.label.schematic_save.checkbox.ignore_entities");
        this.checkboxIgnoreEntities = new WidgetCheckBox(x, y + 24, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, str);
        this.addWidget(this.checkboxIgnoreEntities);

        this.checkboxVisibleOnly = new WidgetCheckBox(12, y + 24, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, "Visible blocks only [experimental quick hax]");
        this.addWidget(this.checkboxVisibleOnly);

        this.checkboxSaveFromSchematicWorld.setPosition(20 + this.checkboxVisibleOnly.getWidth(), y + 24);
        this.addWidget(this.checkboxSaveFromSchematicWorld);

        this.createButton(x, y);
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

    protected abstract IButtonActionListener createButtonListener();

    private void createButton(int x, int y)
    {
        String label = StringUtils.translate(ButtonType.SAVE.getLabelKey());
        int width = this.getStringWidth(label) + 10;

        ButtonGeneric button;

        button = new ButtonGeneric(x, y, width, 20, label, "litematica.gui.label.schematic_save.hoverinfo.hold_shift_to_overwrite");

        this.addButton(button, this.createButtonListener());

    }

    @Override
    public void setString(String string)
    {
        this.setNextMessageType(MessageType.ERROR);
        super.setString(string);
    }

    @Override
    public void drawContents(DrawContext context, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(context, mouseX, mouseY, partialTicks);

        this.textField.render(context, mouseX, mouseY, partialTicks);
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
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (this.textField.keyPressed(keyCode, scanCode, modifiers))
        {
            this.getListWidget().clearSelection();
            return true;
        }
        else if (keyCode == KeyCodes.KEY_TAB)
        {
            this.textField.setFocused(! this.textField.isFocused());
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char charIn, int modifiers)
    {
        if (this.textField.charTyped(charIn, modifiers))
        {
            this.getListWidget().clearSelection();
            return true;
        }

        return super.onCharTyped(charIn, modifiers);
    }

    public enum ButtonType
    {
        SAVE ("litematica.gui.button.save_schematic");

        private final String labelKey;

        ButtonType(String labelKey)
        {
            this.labelKey = labelKey;
        }

        public String getLabelKey()
        {
            return this.labelKey;
        }
    }
}
