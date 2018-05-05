package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.SchematicHolder.SchematicEntry;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.widgets.WidgetLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import net.minecraft.client.resources.I18n;

public class GuiLoadedSchematicsManager extends GuiListBase<SchematicEntry, WidgetSchematicEntry, WidgetLoadedSchematics>
{
    private int id;

    public GuiLoadedSchematicsManager()
    {
        super(10, 40);

        this.title = I18n.format("litematica.gui.title.manage_loaded_schematics");
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
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = this.height - 36;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.parent));
        x += buttonWidth + 4;

        type = ButtonListenerChangeMenu.ButtonType.SHOW_PLACEMENTS;
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

    @Override
    protected WidgetLoadedSchematics createListWidget(int listX, int listY)
    {
        return new WidgetLoadedSchematics(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this, null);
    }
}
