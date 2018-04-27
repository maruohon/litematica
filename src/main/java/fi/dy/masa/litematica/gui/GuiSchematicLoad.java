package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntryType;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiSchematicLoad extends GuiSchematicBrowserBase
{
    public GuiSchematicLoad()
    {
        super(10, 40);
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.load_schematic");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = this.height - 36;
        int buttonWidth;
        int id = 0;
        String label;
        ButtonGeneric button;

        label = I18n.format("litematica.gui.button.load_schematic_to_memory");
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.LOAD_SCHEMATIC, this));
        x += buttonWidth + 4;

        label = I18n.format("litematica.gui.button.schematic_actions.show_loaded_schematics");
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.SHOW_LOADED, this));

        label = I18n.format("litematica.gui.button.to_main_menu");
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.MAIN_MENU, this));
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final GuiSchematicLoad gui;

        public ButtonListener(Type type, GuiSchematicLoad gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.LOAD_SCHEMATIC)
            {
                DirectoryEntry entry = this.gui.widget.getSelectedEntry();

                if (entry == null)
                {
                    this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.no_schematic_selected"));
                    return;
                }

                File file = entry.getFullPath();

                if (file.exists() == false || file.isFile() == false || file.canRead() == false)
                {
                    this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.cant_read_file", file.getName()));
                    return;
                }

                if (entry.getType() == DirectoryEntryType.LITEMATICA_SCHEMATIC)
                {
                    this.gui.setNextMessageType(InfoType.ERROR);
                    LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName(), this.gui);

                    if (schematic != null)
                    {
                        schematic.getMetadata().setName(file.getName());
                        SchematicHolder.getInstance().addSchematic(schematic);
                        this.gui.addGuiMessage(InfoType.SUCCESS, I18n.format("litematica.info.schematic_load.schematic_loaded", file.getName()));
                    }
                }
                else
                {
                    this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.unsupported_type", file.getName()));
                }
            }
            else if (this.type == Type.SHOW_LOADED)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiLoadedSchematicsManager());
            }
            else if (this.type == Type.MAIN_MENU)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicActions());
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            LOAD_SCHEMATIC,
            SHOW_LOADED,
            MAIN_MENU;
        }
    }
}
