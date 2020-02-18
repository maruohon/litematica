package fi.dy.masa.litematica.gui;

import java.io.File;
import java.util.Collection;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser.CachedSchematicData;
import fi.dy.masa.litematica.materials.MaterialListSchematic;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiStringListSelection;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IStringListConsumer;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiSchematicLoad extends GuiSchematicBrowserBase
{
    public GuiSchematicLoad()
    {
        super(12, 24);

        this.title = StringUtils.translate("litematica.gui.title.load_schematic");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_load";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    public int getMaxInfoHeight()
    {
        return this.getBrowserHeight() + 10;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = this.height - 40;
        int buttonWidth;
        String label;
        ButtonGeneric button;

        label = StringUtils.translate("litematica.gui.label.schematic_load.checkbox.create_placement");
        String hover = StringUtils.translate("litematica.gui.label.schematic_load.hoverinfo.create_placement");
        WidgetCheckBox checkbox = new WidgetCheckBox(x, y, LitematicaGuiIcons.CHECKBOX_UNSELECTED, LitematicaGuiIcons.CHECKBOX_SELECTED, label, hover);
        checkbox.setListener((widget) -> { Configs.Internal.CREATE_PLACEMENT_ON_LOAD.setBooleanValue(widget.isChecked()); });
        checkbox.setChecked(Configs.Internal.CREATE_PLACEMENT_ON_LOAD.getBooleanValue(), false);
        this.addWidget(checkbox);

        y = this.height - 26;
        x += this.createButton(x, y, -1, ButtonListener.Type.LOAD_SCHEMATIC) + 4;
        x += this.createButton(x, y, -1, ButtonListener.Type.MATERIAL_LIST) + 4;

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

    private int createButton(int x, int y, int width, ButtonListener.Type type)
    {
        ButtonListener listener = new ButtonListener(type, this);
        String label = StringUtils.translate(type.getTranslationKey());

        if (width == -1)
        {
            width = this.getStringWidth(label) + 10;
        }

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, label);

        if (type == ButtonListener.Type.MATERIAL_LIST)
        {
            button.addHoverString("litematica.gui.button.hover.material_list_shift_to_select_sub_regions");
        }

        this.addButton(button, listener);

        return width;
    }

    private static class ButtonListener implements IButtonActionListener
    {
        private final Type type;
        private final GuiSchematicLoad gui;

        public ButtonListener(Type type, GuiSchematicLoad gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            DirectoryEntry entry = this.gui.getListWidget().getLastSelectedEntry();

            if (entry == null)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                return;
            }

            File file = entry.getFullPath();
            CachedSchematicData data = this.gui.getListWidget().getCachedSchematicData(file);

            if (data == null)
            {
                this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getName());
                return;
            }

            this.gui.setNextMessageType(MessageType.ERROR);
            ISchematic schematic = data.schematic;

            if (this.type == Type.LOAD_SCHEMATIC)
            {
                SchematicHolder.getInstance().addSchematic(schematic, true);
                this.gui.addMessage(MessageType.SUCCESS, "litematica.info.schematic_load.schematic_loaded", file.getName());

                if (Configs.Internal.CREATE_PLACEMENT_ON_LOAD.getBooleanValue())
                {
                    BlockPos pos = new BlockPos(this.gui.mc.player.getPositionVector());
                    String name = schematic.getMetadata().getName();
                    boolean enabled = GuiBase.isShiftDown() == false;

                    SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
                    SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, name, enabled);
                    manager.addSchematicPlacement(placement, true);
                    manager.setSelectedSchematicPlacement(placement);
                }
            }
            else if (this.type == Type.MATERIAL_LIST)
            {
                if (GuiBase.isShiftDown())
                {
                    MaterialListCreator creator = new MaterialListCreator(schematic);
                    GuiStringListSelection gui = new GuiStringListSelection(schematic.getRegionNames(), creator);
                    gui.setTitle(StringUtils.translate("litematica.gui.title.material_list.select_schematic_regions", schematic.getMetadata().getName()));
                    gui.setParent(GuiUtils.getCurrentScreen());
                    GuiBase.openGui(gui);
                }
                else
                {
                    MaterialListSchematic materialList = new MaterialListSchematic(schematic, true);
                    DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
                    GuiBase.openGui(new GuiMaterialList(materialList));
                }
            }
        }

        public enum Type
        {
            LOAD_SCHEMATIC  ("litematica.gui.button.load_schematic_to_memory"),
            MATERIAL_LIST   ("litematica.gui.button.material_list");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getTranslationKey()
            {
                return this.translationKey;
            }
        }
    }

    private static class MaterialListCreator implements IStringListConsumer
    {
        private final ISchematic schematic;

        public MaterialListCreator(ISchematic schematic)
        {
            this.schematic = schematic;
        }

        @Override
        public boolean consume(Collection<String> strings)
        {
            MaterialListSchematic materialList = new MaterialListSchematic(this.schematic, strings, true);
            DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
            GuiBase.openGui(new GuiMaterialList(materialList));

            return true;
        }
    }
}
