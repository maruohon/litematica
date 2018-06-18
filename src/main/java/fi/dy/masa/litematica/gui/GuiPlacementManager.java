package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiListBase;
import fi.dy.masa.litematica.gui.interfaces.ISelectionListener;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacement;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicPlacements;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiPlacementManager extends GuiListBase<SchematicPlacement, WidgetSchematicPlacement, WidgetSchematicPlacements>  implements ISelectionListener<SchematicPlacement>
{
    private final SchematicPlacementManager manager;
    private int id;

    public GuiPlacementManager()
    {
        super(10, 40);

        this.title = I18n.format("litematica.gui.title.manage_schematic_placements");

        Minecraft mc = Minecraft.getMinecraft();
        int dimension = mc.world.provider.getDimensionType().getId();
        this.manager = DataManager.getInstance(dimension).getSchematicPlacementManager();
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
        buttonWidth = this.fontRenderer.getStringWidth(label) + 30;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label, type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.parent));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.parent));
    }

    @Override
    public void onSelectionChange(SchematicPlacement entry)
    {
        this.manager.setSelectedSchematicPlacement(entry);
    }

    @Override
    protected ISelectionListener<SchematicPlacement> getSelectionListener()
    {
        return this;
    }

    @Override
    protected WidgetSchematicPlacements createListWidget(int listX, int listY)
    {
        return new WidgetSchematicPlacements(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this, this.getSelectionListener());
    }
}
