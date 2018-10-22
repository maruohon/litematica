package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetListMaterialList;
import fi.dy.masa.litematica.gui.widgets.WidgetMaterialListEntry;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.MaterialListEntry;
import fi.dy.masa.litematica.util.MaterialListEntry.SortCriteria;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.resources.I18n;

public class GuiMaterialList extends GuiListBase<MaterialListEntry, WidgetMaterialListEntry, WidgetListMaterialList>
{
    private final SchematicPlacement placement;
    private int id;

    public GuiMaterialList(SchematicPlacement placement)
    {
        super(10, 60);

        String schematicName = placement.getName();
        this.title = I18n.format("litematica.gui.title.material_list", schematicName);
        this.placement = placement;
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 94;
    }

    @Override
    public void initGui()
    {
        WidgetMaterialListEntry.resetNameLengths();

        super.initGui();

        int x = 12;
        int y = 40;
        int buttonWidth;
        this.id = 0;
        String label;
        ButtonGeneric button;

        x += this.createButton(x, y, -1, ButtonListener.Type.SORT_BY_NAME) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SORT_BY_REQUIRED) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.SORT_BY_AVAILABLE) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.TOGGLE_INFO_HUD) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.WRITE_TO_FILE) + 4;
        y += 22;

        y = this.height - 36;
        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this.placement, this);
        String label = "";

        if (type == ButtonListener.Type.TOGGLE_INFO_HUD)
        {
            boolean val = InfoHud.getInstance().isEnabled();
            String str = (val ? TXT_GREEN : TXT_RED) + I18n.format("litematica.message.value." + (val ? "on" : "off")) + TXT_RST;
            label = type.getDisplayName(str);
        }
        else
        {
            label = type.getDisplayName();
        }

        if (width == -1)
        {
            width = this.mc.fontRenderer.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, width, 20, label);
        this.addButton(button, listener);

        return width;
    }

    public SchematicPlacement getSchematicPlacement()
    {
        return this.placement;
    }

    @Override
    protected WidgetListMaterialList createListWidget(int listX, int listY)
    {
        return new WidgetListMaterialList(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMaterialList parent;
        private final Type type;

        public ButtonListener(Type type, SchematicPlacement placement, GuiMaterialList parent)
        {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            switch (this.type)
            {
                case SORT_BY_NAME:
                    MaterialListEntry.setSortCriteria(SortCriteria.NAME);
                    break;

                case SORT_BY_REQUIRED:
                    MaterialListEntry.setSortCriteria(SortCriteria.COUNT_REQUIRED);
                    break;

                case SORT_BY_AVAILABLE:
                    MaterialListEntry.setSortCriteria(SortCriteria.COUNT_AVAILABLE);
                    break;

                case WRITE_TO_FILE:
                    break;

                case TOGGLE_INFO_HUD:
                    break;
            }

            this.parent.initGui(); // Re-create buttons/text fields
        }

        public enum Type
        {
            SORT_BY_NAME        ("litematica.gui.button.material_list.name"),
            SORT_BY_REQUIRED    ("litematica.gui.button.material_list.required"),
            SORT_BY_AVAILABLE   ("litematica.gui.button.material_list.available"),
            WRITE_TO_FILE       ("litematica.gui.button.material_list.write_to_file"),
            TOGGLE_INFO_HUD     ("litematica.gui.button.material_list.toggle_info_hud");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName(Object... args)
            {
                return I18n.format(this.translationKey, args);
            }
        }
    }
}
