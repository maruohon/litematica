package fi.dy.masa.litematica.gui;

import java.util.HashMap;
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
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicPlacementsList
        extends GuiListBase<SchematicPlacementUnloaded, WidgetSchematicPlacementEntry, WidgetListSchematicPlacements>
        implements ISelectionListener<SchematicPlacementUnloaded>
{
    private final SchematicPlacementManager manager;
    private final HashMap<SchematicPlacementUnloaded, Boolean> modifiedCache = new HashMap<>();

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
        this.modifiedCache.clear();

        int x = 12;
        int y = this.height - 26;
        ButtonGeneric button;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS;
        button = new ButtonGeneric(x, y, -1, 20, type.getLabelKey(), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
        x += button.getWidth() + 2;

        button = new ButtonGeneric(x, y, -1, 20, "litematica.gui.label.schematic_placement.open_placement_browser");
        this.addButton(button, (btn, mbtn) -> { GuiBase.openGui((new GuiSchematicPlacementFileBrowser()).setParent(this)); });

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        button = new ButtonGeneric(this.width - 10, y, -1, true, type.getLabelKey());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        x = this.width - 22;
        y = 10;
        String val = Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.getBooleanValue() ? "litematica.gui.label.misc.icons" : "litematica.gui.label.misc.text";
        button = new ButtonGeneric(x, y, -1, true, StringUtils.translate("litematica.gui.button.buttons_val", StringUtils.translate(val)));
        button.addHoverString("litematica.gui.button.hover.use_text_or_icon_buttons");
        this.addButton(button, (btn, mbtn) -> { Configs.Internal.PLACEMENT_LIST_ICON_BUTTONS.toggleBooleanValue(); this.initGui(); });
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

    public boolean getCachedWasModifiedSinceSaved(SchematicPlacementUnloaded placement)
    {
        Boolean modified = this.modifiedCache.get(placement);

        if (modified == null)
        {
            modified = placement.wasModifiedSinceSaved();
            this.modifiedCache.put(placement, modified);
        }

        return modified.booleanValue();
    }
}
