package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.config.gui.button.IButtonActionListener;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.widgets.WidgetLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetLoadedSchematics.SchematicEntry;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiLoadedSchematicsManager extends GuiListBase<SchematicEntry, WidgetSchematicEntry, WidgetLoadedSchematics>
{
    public GuiLoadedSchematicsManager()
    {
        super(10, 40);
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.manage_loaded_schematics");
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

        label = I18n.format(Type.LOAD_SCHEMATICS.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(Type.LOAD_SCHEMATICS));
        x += buttonWidth + 4;

        label = I18n.format(Type.MAIN_MENU.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(Type.MAIN_MENU));
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 80;
    }

    @Override
    protected WidgetLoadedSchematics createListWidget(int listX, int listY)
    {
        return new WidgetLoadedSchematics(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), null);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;

        public ButtonListener(Type type)
        {
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.LOAD_SCHEMATICS)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad());
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
    }

    private enum Type
    {
        LOAD_SCHEMATICS     ("litematica.gui.button.schematic_actions.load_schematic_to_memory"),
        MAIN_MENU           ("litematica.gui.button.to_main_menu");

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
