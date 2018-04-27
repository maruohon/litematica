package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacements;
import net.minecraft.client.resources.I18n;

public class GuiPlacementManager extends GuiListBase<SchematicPlacement, WidgetSchematicPlacement, WidgetSchematicPlacements>
{
    private int id;

    public GuiPlacementManager()
    {
        super(10, 40);
    }

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.manage_schematic_placements");
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

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.SHOW_LOADED;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type));
    }

    @Override
    protected WidgetSchematicPlacements createListWidget(int listX, int listY)
    {
        return new WidgetSchematicPlacements(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this, null);
    }
}
