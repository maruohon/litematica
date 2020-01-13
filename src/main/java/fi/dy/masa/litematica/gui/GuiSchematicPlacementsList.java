package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListSchematicPlacements;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacementEntry;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicPlacementsList
        extends GuiListBase<SchematicPlacementUnloaded, WidgetSchematicPlacementEntry, WidgetListSchematicPlacements>
        implements ISelectionListener<SchematicPlacementUnloaded>
{
    private final SchematicPlacementManager manager;
    protected WidgetCheckBox widgetUseIconButtons;

    public GuiSchematicPlacementsList()
    {
        super(12, 30);

        this.title = StringUtils.translate("litematica.gui.title.manage_schematic_placements");
        this.manager = DataManager.getSchematicPlacementManager();

        // The position is updated in initGui() when the GUI width is known
        this.widgetUseIconButtons = new WidgetCheckBox(20, 36,
                LitematicaGuiIcons.CHECKBOX_UNSELECTED, LitematicaGuiIcons.CHECKBOX_SELECTED, "", "litematica.gui.hover.schematic_placement.checkmark.use_icon_buttons");
        this.widgetUseIconButtons.setChecked(Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.getBooleanValue());
        this.widgetUseIconButtons.setListener((widget) -> { Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.setBooleanValue(widget.isChecked()); this.initGui(); });
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

        button = new ButtonGeneric(this.width - 8, 10, -1, true, "litematica.gui.label.schematic_placement.open_placement_browser");
        this.addButton(button, (btn, mbtn) -> { GuiBase.openGui((new GuiSchematicPlacementFileBrowser()).setParent(this)); });

        this.widgetUseIconButtons.setPosition(this.width - 20, this.widgetUseIconButtons.getY());
        this.addWidget(this.widgetUseIconButtons);
    }

    public boolean useIconButtons()
    {
        return this.widgetUseIconButtons.isChecked();
    }

    @Override
    public void onSelectionChange(@Nullable SchematicPlacementUnloaded entry)
    {
        if (entry == null || entry.isLoaded())
        {
            this.manager.setSelectedSchematicPlacement(entry != this.manager.getSelectedSchematicPlacement() ? (SchematicPlacement) entry : null);
        }
    }

    @Override
    protected ISelectionListener<SchematicPlacementUnloaded> getSelectionListener()
    {
        return this;
    }

    @Override
    protected WidgetListSchematicPlacements createListWidget(int listX, int listY)
    {
        return new WidgetListSchematicPlacements(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }
}
