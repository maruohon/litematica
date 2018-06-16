package fi.dy.masa.litematica.gui;

import java.io.File;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import com.mumfrey.liteloader.client.overlays.IGuiTextField;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class GuiSchematicSave extends GuiSchematicBrowserBase implements ISelectionListener<DirectoryEntry>
{
    private final SelectionManager selectionManager;
    private final GuiTextField textField;
    @Nullable
    private final LitematicaSchematic schematic;

    public GuiSchematicSave()
    {
        this(null);
    }

    public GuiSchematicSave(@Nullable LitematicaSchematic schematic)
    {
        super(10, 60);

        if (schematic != null)
        {
            this.title = I18n.format("litematica.gui.title.save_schematic_from_memory");
        }
        else
        {
            this.title = I18n.format("litematica.gui.title.create_schematic_from_selection");
        }

        Minecraft mc = Minecraft.getMinecraft();
        this.selectionManager = DataManager.getInstance(mc.world).getSelectionManager();
        this.schematic = schematic;

        this.textField = new GuiTextField(0, mc.fontRenderer, 10, 32, 90, 20);
        this.textField.setMaxStringLength(256);
        this.textField.setText("");
        this.textField.setFocused(true);
        this.textField.setCursorPositionEnd();
    }

    @Override
    public void initGui()
    {
        super.initGui();

        ((IGuiTextField) this.textField).setInternalWidth(this.width - 273);
        DirectoryEntry entry = this.widget.getSelectedEntry();

        if (entry != null)
        {
            this.textField.setText(FileUtils.getNameWithoutExtension(entry.getName()));
        }
        else if (this.schematic != null)
        {
            this.textField.setText(this.schematic.getMetadata().getName());
        }

        int x = this.textField.x + this.textField.getWidth() + 12;
        int y = 32;

        this.createButton(1, x     , y,  80, ButtonListener.Type.SAVE);
        this.createButton(2, x + 84, y, 140, ButtonListener.Type.CREATE_DIRECTORY);
    }

    private void createButton(int id, int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.selectionManager, this);
        String label = "";

        if (type == ButtonListener.Type.SAVE)
        {
            label = "litematica.gui.button.save_area_to_schematic";
        }
        else if (type == ButtonListener.Type.CREATE_DIRECTORY)
        {
            label = "litematica.gui.button.create_directory";
        }

        ButtonGeneric button = new ButtonGeneric(id, x, y, width, 20, I18n.format(label));
        this.addButton(button, listener);
    }

    @Override
    public void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(mouseX, mouseY, partialTicks);

        this.textField.drawTextBox();
    }

    @Override
    public void setString(String string)
    {
        this.setNextMessageType(InfoType.ERROR);
        super.setString(string);
    }

    @Override
    public void onSelectionChange(DirectoryEntry entry)
    {
        if (entry != null)
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
        boolean ret = this.textField.mouseClicked(mouseX, mouseY, mouseButton);

        int x = this.textField.x;
        int y = this.textField.y;
        int w = ((IGuiTextField) this.textField).getInternalWidth();
        int h = ((IGuiTextField) this.textField).getHeight();

        // Clear the field on right click
        if (mouseButton == 1 && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h)
        {
            this.textField.setText("");
            this.textField.setCursorPosition(0);
            ret = true;
        }

        if (ret)
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
            this.widget.clearSelection();
            return true;
        }
        else if (keyCode == Keyboard.KEY_TAB)
        {
            this.textField.setFocused(! this.textField.isFocused());
            return true;
        }

        return super.onKeyTyped(typedChar, keyCode);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSchematicSave gui;
        private final SelectionManager selectionManager;
        private final Type type;

        public ButtonListener(Type type, SelectionManager selectionManager, GuiSchematicSave gui)
        {
            this.type = type;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.SAVE)
            {
                File dir = this.gui.widget.getCurrentDirectory();
                String name = this.gui.textField.getText();

                if (dir.isDirectory() == false)
                {
                    this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_save.invalid_directory", dir.getAbsolutePath());
                    return;
                }

                if (name.isEmpty())
                {
                    this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", name);
                    return;
                }

                boolean takeEntities = true; // TODO
                LitematicaSchematic schematic = this.gui.schematic;

                if (schematic == null)
                {
                    schematic = this.createSchematicFromWorld(name, takeEntities);
                }
                else
                {
                    schematic.getMetadata().setTimeModified(System.currentTimeMillis());
                }

                if (schematic != null)
                {
                    if (schematic.writeToFile(dir, name, GuiScreen.isShiftKeyDown(), this.gui))
                    {
                        this.gui.addMessage(InfoType.SUCCESS, "litematica.message.schematic_saved_as", name);
                        this.gui.widget.refreshEntries();
                    }
                }
                else
                {
                    this.gui.addMessage(InfoType.ERROR, "litematica.error.schematic_save.schematic_creation_failed");
                }
            }
            else if (this.type == Type.CREATE_DIRECTORY)
            {
                File dir = this.gui.widget.getCurrentDirectory();
                String title = "litematica.gui.title.create_directory";
                this.gui.mc.displayGuiScreen(new GuiTextInput(256, title, "", this.gui, new DirectoryCreator(dir, this.gui)));
            }
            else if (this.type == Type.THUMBNAIL)
            {
                GuiScreen currentScreen = this.gui.mc.currentScreen;
                this.gui.mc.displayGuiScreen(null);
                this.gui.mc.displayGuiScreen(currentScreen);
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        @Nullable
        private LitematicaSchematic createSchematicFromWorld(String name, boolean takeEntities)
        {
            AreaSelection area = this.selectionManager.getCurrentSelection();

            if (area != null)
            {
                String author = this.gui.mc.player.getName();
                LitematicaSchematic schematic = LitematicaSchematic.createSchematic(this.gui.mc.world, area, takeEntities, author, this.gui);

                if (schematic != null)
                {
                    schematic.getMetadata().setName(name);
                }

                return schematic;
            }

            return null;
        }

        public enum Type
        {
            SAVE,
            CREATE_DIRECTORY,
            THUMBNAIL;
        }
    }

    public static class DirectoryCreator implements IStringConsumer
    {
        private final File dir;
        private final GuiSchematicSave parent;

        public DirectoryCreator(File dir, GuiSchematicSave parent)
        {
            this.dir = dir;
            this.parent = parent;
        }

        @Override
        public void setString(String string)
        {
            if (this.dir.exists() == false)
            {
                this.parent.addMessage(InfoType.ERROR, "litematica.error.schematic_save.directory_doesnt_exist", this.dir.getAbsolutePath());
                return;
            }

            if (string.isEmpty())
            {
                this.parent.addMessage(InfoType.ERROR, "litematica.error.schematic_save.invalid_directory", string);
                return;
            }

            File file = new File(this.dir, string);

            if (file.exists())
            {
                this.parent.addMessage(InfoType.ERROR, "litematica.error.schematic_save.file_or_directory_already_exists", file.getAbsolutePath());
                return;
            }

            if (file.mkdirs() == false)
            {
                this.parent.addMessage(InfoType.ERROR, "litematica.error.schematic_save.failed_to_create_directory", file.getAbsolutePath());
                return;
            }

            this.parent.addMessage(InfoType.SUCCESS, "litematica.message.directory_created", string);
        }
    }
}
