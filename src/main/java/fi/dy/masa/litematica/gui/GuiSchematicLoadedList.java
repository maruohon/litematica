package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListLoadedSchematics;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicEntry;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicLoadedList extends BaseListScreen<ISchematic, WidgetSchematicEntry, WidgetListLoadedSchematics>
{
    public GuiSchematicLoadedList()
    {
        super(12, 30, 20, 68);

        this.title = StringUtils.translate("litematica.gui.title.manage_loaded_schematics");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = this.height - 26;
        GenericButton button;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS;
        button = new GenericButton(x, y, -1, 20, type.getLabelKey(), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
        x += button.getWidth() + 4;

        type = ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PLACEMENTS;
        button = new GenericButton(x, y, -1, 20, type.getLabelKey(), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        x = this.width - 10;
        button = new GenericButton(x, y, -1, true, type.getLabelKey());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        x = this.width - 22;
        y = 10;
        String val = Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.getBooleanValue() ? "litematica.gui.label.misc.icons" : "litematica.gui.label.misc.text";
        button = new GenericButton(x, y, -1, true, StringUtils.translate("litematica.gui.button.buttons_val", StringUtils.translate(val)));
        button.addHoverString("litematica.gui.button.hover.use_text_or_icon_buttons");
        this.addButton(button, (btn, mbtn) -> { Configs.Internal.SCHEMATIC_LIST_ICON_BUTTONS.toggleBooleanValue(); this.initGui(); });
    }

    @Override
    protected WidgetListLoadedSchematics createListWidget(int listX, int listY)
    {
        return new WidgetListLoadedSchematics(listX, listY, this.getListWidth(), this.getListHeight());
    }
}
