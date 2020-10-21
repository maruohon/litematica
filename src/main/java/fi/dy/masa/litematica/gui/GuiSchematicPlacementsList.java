package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicPlacementsList extends GuiListBase<SchematicPlacement, WidgetSchematicPlacement, WidgetListSchematicPlacements> implements ISelectionListener<SchematicPlacement>
{
    public final SchematicPlacementManager manager;

    public GuiSchematicPlacementsList()
    {
        super(12, 30);

        this.title = StringUtils.translate("litematica.gui.title.manage_schematic_placements");
        this.manager = DataManager.getSchematicPlacementManager();
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 64;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = this.height - 26;
        int buttonWidth;
        String label;
        ButtonGeneric button;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS;
        label = StringUtils.translate(type.getLabelKey());
        buttonWidth = this.getStringWidth(label) + 30;
        button = new ButtonGeneric(x, y, buttonWidth, 20, label, type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = StringUtils.translate(type.getLabelKey());
        buttonWidth = this.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    @Override
    public void onSelectionChange(@Nullable SchematicPlacement entry)
    {
        this.manager.setSelectedSchematicPlacement(entry != this.manager.getSelectedSchematicPlacement() ? entry : null);
    }

    @Override
    protected ISelectionListener<SchematicPlacement> getSelectionListener()
    {
        return this;
    }

    @Override
    protected WidgetListSchematicPlacements createListWidget(int listX, int listY)
    {
        return new WidgetListSchematicPlacements(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }
}
