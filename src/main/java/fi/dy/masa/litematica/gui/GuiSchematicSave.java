package fi.dy.masa.litematica.gui;

import java.io.File;
import java.io.IOException;
import org.lwjgl.input.Keyboard;
import com.mumfrey.liteloader.client.overlays.IGuiTextField;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.selection.Selection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.FileUtils;
import fi.dy.masa.litematica.util.InfoUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

public class GuiSchematicSave extends GuiSchematicBrowserBase
{
    private final SelectionManager selectionManager;
    private final GuiTextField textField;

    public GuiSchematicSave()
    {
        super(10, 60);

        Minecraft mc = Minecraft.getMinecraft();
        this.selectionManager = DataManager.getInstance(mc.world).getSelectionManager();

        this.textField = new GuiTextField(0, mc.fontRenderer, 10, 32, 100, 20);
        this.textField.setMaxStringLength(160);
        this.textField.setText("");
        this.textField.setFocused(true);
        this.textField.setCursorPositionEnd();
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.save_schematic");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        ((IGuiTextField) this.textField).setInternalWidth(this.width - 253);
        DirectoryEntry entry = this.schematicBrowser.getSelectedEntry();

        if (entry != null)
        {
            this.textField.setText(FileUtils.getNameWithoutExtension(entry.getName()));
        }

        int x = this.textField.x + this.textField.getWidth() + 12;
        int y = 32;
        String label = I18n.format("litematica.gui.button.save_area_to_schematic");
        ButtonGeneric button = new ButtonGeneric(1, x, y, 60, 20, label);
        ButtonListener listener = this.createActionListener(ButtonListener.Type.SAVE);
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
        this.textField.setText(FileUtils.getNameWithoutExtension(string));
        this.textField.setCursorPositionEnd();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        this.textField.mouseClicked(mouseX, mouseY, mouseButton);

        int x = this.textField.x;
        int y = this.textField.y;
        int w = ((IGuiTextField) this.textField).getInternalWidth();
        int h = ((IGuiTextField) this.textField).getHeight();

        // Clear the field on right click
        if (mouseButton == 1 && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h)
        {
            this.textField.setText("");
            this.textField.setCursorPosition(0);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        super.keyTyped(typedChar, keyCode);

        if (this.textField.textboxKeyTyped(typedChar, keyCode))
        {
            this.schematicBrowser.setSelectedEntry(null, -1);
        }
        else if (keyCode == Keyboard.KEY_TAB)
        {
            this.textField.setFocused(! this.textField.isFocused());
        }
    }

    private ButtonListener createActionListener(ButtonListener.Type type)
    {
        return new ButtonListener(type, this.selectionManager, this);
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
                IStringConsumer feedback = InfoUtils.INFO_MESSAGE_CONSUMER;
                File dir = this.gui.schematicBrowser.getCurrentDirectory();
                String name = this.gui.textField.getText();

                if (dir.isDirectory() == false)
                {
                    feedback.setString(I18n.format("litematica.error.message.invalid_directory", dir.getAbsolutePath()));
                    return;
                }

                if (name.isEmpty())
                {
                    feedback.setString(I18n.format("litematica.error.message.invalid_schematic_name", name));
                    return;
                }

                File file = new File(dir, name);
                boolean takeEntities = true; // TODO
                SchematicSaver saver = new SchematicSaver(dir, name, takeEntities, this.gui.mc, this.selectionManager, this.gui);

                if (file.exists())
                {
                    
                }
                else
                {
                    saver.saveSchematic();
                }
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

        public enum Type
        {
            SAVE,
            THUMBNAIL;
        }
    }

    private static class SchematicSaver
    {
        private final GuiSchematicSave gui;
        private final File dir;
        private final String name;
        private final Minecraft mc;
        private final SelectionManager selectionManager;
        private final boolean takeEntities;

        public SchematicSaver(File dir, String name, boolean takeEntities, Minecraft mc, SelectionManager selectionManager, GuiSchematicSave gui)
        {
            this.dir = dir;
            this.name = name;
            this.takeEntities = takeEntities;
            this.mc = mc;
            this.selectionManager = selectionManager;
            this.gui = gui;
        }

        public void saveSchematic()
        {
            Selection area = this.selectionManager.getCurrentSelection();

            if (area != null)
            {
                IStringConsumer feedback = InfoUtils.INFO_MESSAGE_CONSUMER;
                String author = this.mc.player.getName();
                LitematicaSchematic schematic = LitematicaSchematic.makeSchematic(this.mc.world, area, this.takeEntities, author, feedback);

                if (schematic != null)
                {
                    if (schematic.writeToFile(this.dir, this.name, true, feedback))
                    {
                        feedback.setString("litematica.message.schematic_saved");
                        this.gui.schematicBrowser.refreshEntries();
                    }
                }
            }
        }
    }
}
