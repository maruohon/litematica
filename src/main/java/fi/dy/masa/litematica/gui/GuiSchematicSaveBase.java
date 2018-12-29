package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import com.mumfrey.liteloader.client.overlays.IGuiTextField;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IDirectoryNavigator;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntryType;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.util.FileUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public abstract class GuiSchematicSaveBase extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    protected final GuiTextField textField;
    @Nullable
    protected final LitematicaSchematic schematic;
    protected WidgetCheckBox checkboxIgnoreEntities;
    protected String defaultText = "";

    public GuiSchematicSaveBase(@Nullable LitematicaSchematic schematic)
    {
        super(10, 70);

        this.schematic = schematic;

        Minecraft mc = Minecraft.getMinecraft();
        this.textField = new GuiTextFieldGeneric(0, mc.fontRenderer, 10, 32, 90, 20);
        this.textField.setMaxStringLength(256);
        this.textField.setFocused(true);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        ((IGuiTextField) this.textField).setInternalWidth(this.width - 273);
        DirectoryEntry entry = this.getListWidget().getLastSelectedEntry();

        if (entry != null && entry.getType() != DirectoryEntryType.DIRECTORY)
        {
            this.textField.setText(FileUtils.getNameWithoutExtension(entry.getName()));
        }
        else if (this.schematic != null)
        {
            this.textField.setText(this.schematic.getMetadata().getName());
        }
        else
        {
            this.textField.setText(this.defaultText);
        }

        this.textField.setCursorPositionEnd();

        int x = this.textField.x + this.textField.getWidth() + 12;
        int y = 32;

        String str = I18n.format("litematica.gui.label.schematic_save.checkbox.ignore_entities");
        this.checkboxIgnoreEntities = new WidgetCheckBox(x, y + 24, this.zLevel, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, str, this.mc);
        this.addWidget(this.checkboxIgnoreEntities);

        x = this.createButton(1, x, y, ButtonType.SAVE);
        x = this.createButton(2, x, y, ButtonType.CREATE_DIRECTORY);
    }

    protected abstract IButtonActionListener<ButtonGeneric> createButtonListener(ButtonType type);

    private int createButton(int id, int x, int y, ButtonType type)
    {
        String label = I18n.format(type.getLabelKey());
        int width = this.mc.fontRenderer.getStringWidth(label) + 10;

        ButtonGeneric button;

        if (type == ButtonType.SAVE)
        {
            button = new ButtonGeneric(id, x, y, width, 20, label, "litematica.gui.label.schematic_save.hoverinfo.hold_shift_to_overwrite");
        }
        else
        {
            button = new ButtonGeneric(id, x, y, width, 20, label);
        }

        this.addButton(button, this.createButtonListener(type));

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
            this.textField.setText(FileUtils.getNameWithoutExtension(entry.getName()));
            this.textField.setCursorPositionEnd();
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
        if (this.textField.textboxKeyTyped(typedChar, keyCode))
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

    public static class DirectoryCreator implements IStringConsumerFeedback
    {
        private final File dir;
        private final GuiBase parent;
        private final IDirectoryNavigator navigator;

        public DirectoryCreator(File dir, GuiBase parent, IDirectoryNavigator navigator)
        {
            this.dir = dir;
            this.parent = parent;
            this.navigator = navigator;
        }

        @Override
        public boolean setString(String string)
        {
            if (string.isEmpty())
            {
                this.parent.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", string);
                return false;
            }

            File file = new File(this.dir, string);

            if (file.exists())
            {
                this.parent.addMessage(MessageType.ERROR, "litematica.error.schematic_save.file_or_directory_already_exists", file.getAbsolutePath());
                return false;
            }

            if (file.mkdirs() == false)
            {
                this.parent.addMessage(MessageType.ERROR, "litematica.error.schematic_save.failed_to_create_directory", file.getAbsolutePath());
                return false;
            }

            this.navigator.switchToDirectory(file);
            this.parent.addMessage(MessageType.SUCCESS, "litematica.message.directory_created", string);

            return true;
        }
    }

    public enum ButtonType
    {
        SAVE                ("litematica.gui.button.save_schematic"),
        CREATE_DIRECTORY    ("litematica.gui.button.create_directory");

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
