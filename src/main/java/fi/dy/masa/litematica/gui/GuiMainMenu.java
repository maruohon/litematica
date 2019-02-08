package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiMainMenu extends GuiBase
{
    public GuiMainMenu()
    {
        this.title = I18n.format("litematica.gui.title.litematica_main_menu");
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
        y += 44;

        String label = I18n.format("litematica.gui.button.tool_mode", DataManager.getToolMode().getName());
        int width2 = this.mc.fontRenderer.getStringWidth(label) + 10;

        y = this.height - 26;
        ButtonGeneric button = new ButtonGeneric(0, x, y, width2, 20, label);
        this.addButton(button, new ButtonListenerCycleToolMode(this));

        int x2 = x + width2 + 4;
        SelectionMode mode = DataManager.getSelectionManager().getSelectionMode();
        label = I18n.format("litematica.gui.button.area_selection_mode", mode.getDisplayName());
        width2 = this.mc.fontRenderer.getStringWidth(label) + 10;
        button = new ButtonGeneric(0, x2, y, width2, 20, label);
        this.addButton(button, new ButtonListenerCycleAreaMode(this));

        x += width + 20;
        y = 30;
        this.createChangeMenuButton(x, y, width, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_MANAGER);
    }

    private void createChangeMenuButton(int x, int y, int width, ButtonListenerChangeMenu.ButtonType type)
    {
        ButtonGeneric button = new ButtonGeneric(0, x, y, width, 20, type.getDisplayName(), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this));
    }

    private int getButtonWidth()
    {
        int width = 0;

        for (ButtonListenerChangeMenu.ButtonType type : ButtonListenerChangeMenu.ButtonType.values())
        {
            width = Math.max(width, this.mc.fontRenderer.getStringWidth(type.getDisplayName()) + 30);
        }

        return width;
    }

    public static class ButtonListenerChangeMenu implements IButtonActionListener<ButtonGeneric>
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
        public void actionPerformed(ButtonGeneric control)
        {
            GuiBase gui = null;

            switch (this.type)
            {
                case AREA_EDITOR:
                    gui = DataManager.getSelectionManager().getEditGui();
                    break;
                case AREA_SELECTION_BROWSER:
                    gui = new GuiAreaSelectionManager();
                    break;
                case LOAD_SCHEMATICS:
                    gui = new GuiSchematicLoad();
                    break;
                case LOADED_SCHEMATICS:
                    gui = new GuiLoadedSchematicsManager();
                    break;
                case MAIN_MENU:
                    gui = new GuiMainMenu();
                    break;
                case SCHEMATIC_MANAGER:
                    gui = new GuiSchematicManager();
                    break;
                case SCHEMATIC_PLACEMENTS:
                    gui = new GuiPlacementManager();
                    break;
            }

            if (gui != null)
            {
                gui.setParent(this.parent);
                Minecraft.getMinecraft().displayGuiScreen(gui);
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            // List loaded Schematics in SchematicHolder
            LOADED_SCHEMATICS       ("litematica.gui.button.change_menu.show_loaded_schematics", ButtonIcons.LOADED_SCHEMATICS),
            // List Schematics placements
            SCHEMATIC_PLACEMENTS    ("litematica.gui.button.change_menu.show_schematic_placements", ButtonIcons.SCHEMATIC_PLACEMENTS),
            // Open the Area Selection browser
            AREA_SELECTION_BROWSER  ("litematica.gui.button.change_menu.show_area_selections", ButtonIcons.AREA_SELECTION),
            // Open the Area Editor GUI
            AREA_EDITOR             ("litematica.gui.button.change_menu.area_editor", ButtonIcons.AREA_EDITOR),
            // Load Schematics from file to memory
            LOAD_SCHEMATICS         ("litematica.gui.button.change_menu.load_schematics_to_memory", ButtonIcons.SCHEMATIC_BROWSER),
            // Edit Schematics (description or icon), or convert between formats
            SCHEMATIC_MANAGER       ("litematica.gui.button.change_menu.schematic_manager", ButtonIcons.SCHEMATIC_MANAGER),
            // Switch to the Litematica main menu
            MAIN_MENU               ("litematica.gui.button.change_menu.to_main_menu", null);

            private final String labelKey;
            private final ButtonIcons icon;

            private ButtonType(String labelKey, ButtonIcons icon)
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
                return I18n.format(this.getLabelKey());
            }

            public ButtonIcons getIcon()
            {
                return this.icon;
            }
        }
    }

    private static class ButtonListenerCycleToolMode implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMainMenu gui;

        private ButtonListenerCycleToolMode(GuiMainMenu gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            this.actionPerformedWithButton(control, 0);
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            ToolMode mode = DataManager.getToolMode().cycle(Minecraft.getMinecraft().player, mouseButton == 0);
            DataManager.setToolMode(mode);
            this.gui.initGui();
        }
    }

    private static class ButtonListenerCycleAreaMode implements IButtonActionListener<ButtonGeneric>
    {
        private final GuiMainMenu gui;

        private ButtonListenerCycleAreaMode(GuiMainMenu gui)
        {
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            this.actionPerformedWithButton(control, 0);
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            SelectionManager manager = DataManager.getSelectionManager();
            manager.setMode(manager.getSelectionMode().cycle(true));
            this.gui.initGui();
        }
    }
}
