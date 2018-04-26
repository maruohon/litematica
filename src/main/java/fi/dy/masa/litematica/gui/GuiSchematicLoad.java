package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.WidgetSchematicBrowser.DirectoryEntry;
import fi.dy.masa.litematica.gui.WidgetSchematicBrowser.DirectoryEntryType;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicHolder;
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
        String label = I18n.format("litematica.gui.button.load_schematic_to_memory");
        int lw = this.fontRenderer.getStringWidth(label);
        ButtonGeneric button = new ButtonGeneric(1, x, y, lw + 30, 20, label);
        ButtonListener listener = new ButtonListener(this);
        this.addButton(button, listener);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiSchematicLoad gui;

        public ButtonListener(GuiSchematicLoad gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            DirectoryEntry entry = this.gui.schematicBrowser.getSelectedEntry();

            if (entry == null)
            {
                this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.no_schematic_selected"));
                return;
            }

            File file = entry.getFullPath();

            if (file.exists() == false || file.isFile() == false || file.canRead() == false)
            {
                this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.cant_read_file"));
                return;
            }

            if (entry.getType() == DirectoryEntryType.LITEMATICA_SCHEMATIC)
            {
                this.gui.setNextMessageType(InfoType.ERROR);
                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName(), this.gui);

                if (schematic != null)
                {
                    SchematicHolder.getInstance().addSchematic(schematic);
                    this.gui.addGuiMessage(InfoType.SUCCESS, I18n.format("litematica.info.schematic_load.schematic_loaded"));
                }
            }
            else
            {
                this.gui.addGuiMessage(InfoType.ERROR, I18n.format("litematica.error.schematic_load.unsupported_type"));
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }
    }
}
