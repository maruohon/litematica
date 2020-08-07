package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.button.BaseButton;
import fi.dy.masa.malilib.gui.button.GenericButton;
import fi.dy.masa.malilib.gui.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.util.StringUtils;

public class GuiMainMenu extends BaseScreen
{
    public GuiMainMenu()
    {
        String version = String.format("v%s", Reference.MOD_VERSION);
        this.title = StringUtils.translate("litematica.gui.title.litematica_main_menu", version);
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 12;
        int y = 30;
        int width = this.getButtonWidth();

        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PLACEMENTS);
        y += 22;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS);
        y += 22;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS);
        y += 44;

        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.AREA_EDITOR);
        y += 22;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.AREA_SELECTION_BROWSER);
        y += 22;

        SelectionMode mode = DataManager.getSelectionManager().getSelectionMode();
        String label = StringUtils.translate("litematica.gui.button.area_selection_mode", mode.getDisplayName());
        GenericButton button = new GenericButton(x, y, width, 20, label);
        this.addButton(button, new ButtonListenerCycleAreaMode(this));

        label = StringUtils.translate("litematica.gui.button.tool_mode", DataManager.getToolMode().getName());
        int width2 = this.getStringWidth(label) + 10;

        y = this.height - 26;
        button = new GenericButton(x, y, width2, 20, label);
        this.addButton(button, new ButtonListenerCycleToolMode(this));

        x += width + 20;
        y = 30;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.CONFIGURATION);
        y += 44;

        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_MANAGER);
        y += 44;

        y += 22;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.TASK_MANAGER);
        y += 22;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PROJECTS_MANAGER);
    }

    private void createChangeMenuButton(int x, int y, int width, ButtonListenerChangeMenu.ButtonType type)
    {
        GenericButton button = new GenericButton(x, y, width, 20, type.getDisplayName(), type.getIcon());

        if (type == ButtonListenerChangeMenu.ButtonType.AREA_SELECTION_BROWSER &&
            DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            button.setEnabled(false);
            button.addHoverString("litematica.gui.button.hover.schematic_projects.area_browser_disabled_currently_in_projects_mode");
        }

        this.addButton(button, new ButtonListenerChangeMenu(type, this));
    }

    private int getButtonWidth()
    {
        int width = 0;

        for (ButtonListenerChangeMenu.ButtonType type : ButtonListenerChangeMenu.ButtonType.values())
        {
            width = Math.max(width, this.getStringWidth(type.getDisplayName()) + 30);
        }

        for (SelectionMode mode : SelectionMode.values())
        {
            String label = StringUtils.translate("litematica.gui.button.area_selection_mode", mode.getDisplayName());
            width = Math.max(width, this.getStringWidth(label) + 10);
        }

        return width;
    }

    public static class ButtonListenerChangeMenu implements ButtonActionListener
    {
        private final ButtonType type;
        @Nullable
        private final GuiScreen parent;

        public ButtonListenerChangeMenu(ButtonType type, @Nullable GuiScreen parent)
        {
            this.type = type;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            BaseScreen gui = null;

            switch (this.type)
            {
                case AREA_EDITOR:
                    gui = DataManager.getSelectionManager().getEditGui();
                    break;
                case AREA_SELECTION_BROWSER:
                    gui = new GuiAreaSelectionManager();
                    break;
                case CONFIGURATION:
                    BaseScreen.openGui(new ConfigScreen());
                    return;
                case LOAD_SCHEMATICS:
                    gui = new GuiSchematicLoad();
                    break;
                case LOADED_SCHEMATICS:
                    gui = new GuiSchematicLoadedList();
                    break;
                case MAIN_MENU:
                    gui = new GuiMainMenu();
                    break;
                case SCHEMATIC_MANAGER:
                    gui = new GuiSchematicManager();
                    break;
                case SCHEMATIC_PLACEMENTS:
                    gui = new GuiSchematicPlacementsList();
                    break;
                case TASK_MANAGER:
                    gui = new GuiTaskManager();
                    break;
                case SCHEMATIC_PROJECTS_MANAGER:
                    DataManager.getSchematicProjectsManager().openSchematicProjectsGui();
                    return;
            }

            if (gui != null)
            {
                gui.setParent(this.parent);
                BaseScreen.openGui(gui);
            }
        }

        public enum ButtonType
        {
            // List loaded Schematics in SchematicHolder
            LOADED_SCHEMATICS           ("litematica.gui.button.change_menu.show_loaded_schematics", LitematicaGuiIcons.LOADED_SCHEMATICS),
            // List Schematics placements
            SCHEMATIC_PLACEMENTS        ("litematica.gui.button.change_menu.show_schematic_placements", LitematicaGuiIcons.SCHEMATIC_PLACEMENTS),
            // Open the Area Selection browser
            AREA_SELECTION_BROWSER      ("litematica.gui.button.change_menu.show_area_selections", LitematicaGuiIcons.AREA_SELECTION),
            // Open the Area Editor GUI
            AREA_EDITOR                 ("litematica.gui.button.change_menu.area_editor", LitematicaGuiIcons.AREA_EDITOR),
            // Load Schematics from file to memory
            LOAD_SCHEMATICS             ("litematica.gui.button.change_menu.load_schematics_to_memory", LitematicaGuiIcons.SCHEMATIC_BROWSER),
            // Edit Schematics (description or icon), or convert between formats
            SCHEMATIC_MANAGER           ("litematica.gui.button.change_menu.schematic_manager", LitematicaGuiIcons.SCHEMATIC_MANAGER),
            // Open the Task Manager
            TASK_MANAGER                ("litematica.gui.button.change_menu.task_manager", LitematicaGuiIcons.TASK_MANAGER),
            // Open the Schematic Projects browser
            SCHEMATIC_PROJECTS_MANAGER  ("litematica.gui.button.change_menu.schematic_projects_manager", LitematicaGuiIcons.SCHEMATIC_PROJECTS),
            // In-game Configuration GUI
            CONFIGURATION               ("litematica.gui.button.change_menu.configuration_menu", LitematicaGuiIcons.CONFIGURATION),
            // Switch to the Litematica main menu
            MAIN_MENU                   ("litematica.gui.button.change_menu.to_main_menu", null);

            private final String labelKey;
            private final IGuiIcon icon;

            private ButtonType(String labelKey, IGuiIcon icon)
            {
                this.labelKey = labelKey;
                this.icon = icon;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.getLabelKey());
            }

            public IGuiIcon getIcon()
            {
                return this.icon;
            }
        }
    }

    private static class ButtonListenerCycleToolMode implements ButtonActionListener
    {
        private final GuiMainMenu gui;

        private ButtonListenerCycleToolMode(GuiMainMenu gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            ToolMode mode = DataManager.getToolMode().cycle(Minecraft.getMinecraft().player, mouseButton == 0);
            DataManager.setToolMode(mode);
            this.gui.initGui();
        }
    }

    private static class ButtonListenerCycleAreaMode implements ButtonActionListener
    {
        private final GuiMainMenu gui;

        private ButtonListenerCycleAreaMode(GuiMainMenu gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            DataManager.getSelectionManager().switchSelectionMode();
            this.gui.initGui();
        }
    }
}
