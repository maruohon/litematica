package fi.dy.masa.litematica.gui;

import java.io.File;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.SchematicHolder;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.data.SchematicPlacementManager;
import fi.dy.masa.litematica.gui.GuiMainMenu.ButtonListenerChangeMenu;
import fi.dy.masa.litematica.gui.base.GuiSchematicBrowserBase;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.FileType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetCheckBox;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class GuiSchematicLoad extends GuiSchematicBrowserBase
{
    private WidgetCheckBox checkboxCreatePlacementOnLoad;
    private int id;

    public GuiSchematicLoad()
    {
        super(10, 34);

        this.title = I18n.format("litematica.gui.title.load_schematic");
    }

    @Override
    public String getBrowserContext()
    {
        return "schematic_load";
    }

    @Override
    public File getDefaultDirectory()
    {
        return DataManager.ROOT_SCHEMATIC_DIRECTORY;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = this.height - 40;
        int buttonWidth;
        this.id = 0;
        String label;
        String str;
        ButtonGeneric button;

        label = I18n.format("litematica.gui.label.schematic_load.checkbox.create_placement");
        str = I18n.format("litematica.gui.label.schematic_load.hoverinfo.create_placement");
        this.checkboxCreatePlacementOnLoad = new WidgetCheckBox(x, y, this.zLevel, Icons.CHECKBOX_UNSELECTED, Icons.CHECKBOX_SELECTED, label, this.mc, str);
        this.checkboxCreatePlacementOnLoad.setListener(new CheckboxListener());
        this.checkboxCreatePlacementOnLoad.setChecked(DataManager.getCreatePlacementOnLoad());
        this.addWidget(this.checkboxCreatePlacementOnLoad);
        y += 14;

        label = I18n.format("litematica.gui.button.load_schematic_to_memory");
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListener(ButtonListener.Type.LOAD_SCHEMATIC, this));
        x += buttonWidth + 4;

        ButtonListenerChangeMenu.ButtonType type = ButtonListenerChangeMenu.ButtonType.SHOW_LOADED;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 30;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label, type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));

        type = ButtonListenerChangeMenu.ButtonType.MAIN_MENU;
        label = I18n.format(type.getLabelKey());
        buttonWidth = this.fontRenderer.getStringWidth(label) + 20;
        x = this.width - buttonWidth - 10;
        button = new ButtonGeneric(this.id++, x, y, buttonWidth, 20, label);
        this.addButton(button, new ButtonListenerChangeMenu(type, this.getParent()));
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final GuiSchematicLoad gui;

        public ButtonListener(Type type, GuiSchematicLoad gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.LOAD_SCHEMATIC)
            {
                DirectoryEntry entry = this.gui.getListWidget().getSelectedEntry();

                if (entry == null)
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_selected");
                    return;
                }

                File file = entry.getFullPath();

                if (file.exists() == false || file.isFile() == false || file.canRead() == false)
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.cant_read_file", file.getName());
                    return;
                }

                this.gui.setNextMessageType(MessageType.ERROR);
                LitematicaSchematic schematic = null;
                FileType fileType = FileType.fromFile(entry.getFullPath());

                if (fileType == FileType.LITEMATICA_SCHEMATIC)
                {
                    schematic = LitematicaSchematic.createFromFile(entry.getDirectory(), entry.getName(), this.gui);
                }
                else if (fileType == FileType.SCHEMATICA_SCHEMATIC)
                {
                    schematic = WorldUtils.convertSchematicaSchematicToLitematicaSchematic(entry.getDirectory(), entry.getName(), false, this.gui);
                }
                else if (fileType == FileType.VANILLA_STRUCTURE)
                {
                    schematic = WorldUtils.convertStructureToLitematicaSchematic(entry.getDirectory(), entry.getName(), false, this.gui);
                }
                else
                {
                    this.gui.addMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_type", file.getName());
                }

                if (schematic != null)
                {
                    SchematicHolder.getInstance().addSchematic(schematic, true);
                    this.gui.addMessage(MessageType.SUCCESS, "litematica.info.schematic_load.schematic_loaded", file.getName());

                    if (this.gui.checkboxCreatePlacementOnLoad.isChecked())
                    {
                        BlockPos pos = new BlockPos(this.gui.mc.player.getPositionVector());
                        String name = schematic.getMetadata().getName();
                        boolean enabled = GuiScreen.isShiftKeyDown() == false;

                        SchematicPlacementManager manager = DataManager.getInstance().getSchematicPlacementManager();
                        SchematicPlacement placement = SchematicPlacement.createFor(schematic, pos, name, enabled, enabled);
                        manager.addSchematicPlacement(placement, this.gui);
                        manager.setSelectedSchematicPlacement(placement);
                    }
                }
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            LOAD_SCHEMATIC
        }
    }

    private static class CheckboxListener implements ISelectionListener<WidgetCheckBox>
    {
        @Override
        public void onSelectionChange(WidgetCheckBox entry)
        {
            DataManager.setCreatePlacementOnLoad(entry.isChecked());
        }
    }
}
