package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.DirectoryEntryType;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.client.resources.I18n;

public class GuiSchematicLoad extends GuiSchematicBrowserBase
{
    private int id;

    public GuiSchematicLoad()
    {
        super(10, 40);

        this.title = I18n.format("litematica.gui.title.load_schematic");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = this.height - 36;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        label = I18n.format("litematica.gui.button.load_schematic_to_memory");
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.LOAD_SCHEMATIC, this));
        x += buttonWidth + 4;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.SHOW_LOADED;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.parent));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.parent));
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
                        SchematicHolder.getInstance().addSchematic(schematic, file.getName());
                        this.gui.addGuiMessage(InfoType.SUCCESS, I18n.format("litematica.info.schematic_load.schematic_loaded", file.getName()));
                    }
                }
                else
                {
                    this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.unsupported_type", file.getName()));
                }
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            LOAD_SCHEMATIC
        }
    }
}
