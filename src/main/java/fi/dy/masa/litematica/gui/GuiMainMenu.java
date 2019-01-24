package fi.dy.masa.litematica.gui;

import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiMainMenu extends GuiBase
{
    private int id;

    public GuiMainMenu()
    {
        this.title = I18n.format("litematica.gui.title.litematica_main_menu");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 20;
        int y = 30;
        this.id = 0;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_PLACEMENTS);
        y += 22;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.LOADED_SCHEMATICS);
        y += 22;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS);
        y += 44;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.AREA_SELECTION_BROWSER);
        y += 22;
        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.AREA_EDITOR);
        y += 44;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.SCHEMATIC_MANAGER);
    }

    private void createChangeMenuButton(int x, int y, ButtonListenerChangeMenu.ButtonType type)
    {
        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, 200, 20, I18n.format(type.getLabelKey()), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type, this));
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
                    DataManager.getSelectionManager().setMode(SelectionMode.SIMPLE); // FIXME remove, for debug
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
            }

            Minecraft.getMinecraft().displayGuiScreen(gui);
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

            public ButtonIcons getIcon()
            {
                return this.icon;
            }
        }
    }
}
