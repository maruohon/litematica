package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiSchematicActions extends GuiLitematicaBase
{
    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.schematic_actions");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 20;
        int y = 30;

        this.createButton(x, y, Type.SHOW_LOADED);
        y += 44;

        this.createButton(x, y, Type.LOAD_SCHEMATICS);
        y += 22;
    }

    private void createButton(int x, int y, Type type)
    {
        ButtonGeneric button = new ButtonGeneric(0, x, y, 160, 20, I18n.format(type.getLabelKey()));
        this.addButton(button, new ButtonListener(type, this));
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiLitematicaBase gui;
        private final Type type;

        public ButtonListener(Type type, GuiLitematicaBase gui)
        {
            this.gui = gui;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.SHOW_LOADED)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiLoadedSchematicsManager());
            }
            else if (this.type == Type.LOAD_SCHEMATICS)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad());
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }
    }

    private enum Type
    {
        // List loaded Schematics in SchematicHolder
        SHOW_LOADED         ("litematica.gui.button.schematic_actions.show_loaded_schematics"),
        // Load Schematics from file to memory
        LOAD_SCHEMATICS     ("litematica.gui.button.schematic_actions.load_schematic_to_memory"),
        // Create a new Schematic from an area selection
        CREATE_SCHEMATIC    ("litematica.gui.button.schematic_actions.create_schematic_from_area"),
        // Edit Schematics (description or icon), or convert between formats
        MANAGE_SCHEMATICS   ("litematica.gui.button.schematic_actions.manage_schematics");

        private final String labelKey;

        private Type(String labelKey)
        {
            this.labelKey = labelKey;
        }

        public String getLabelKey()
        {
            return this.labelKey;
        }
    }
}
