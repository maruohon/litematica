package fi.dy.masa.litematica.gui;

import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.button.ButtonGeneric;
import fi.dy.masa.litematica.gui.button.IButtonActionListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiMainMenu extends GuiLitematicaBase
{
    private int id;

    @Override
    protected String getTitle()
    {
        return I18n.format("litematica.gui.title.litematica_main_menu");
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 20;
        int y = 30;
        this.id = 0;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.SHOW_PLACEMENTS);
        y += 22;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.SHOW_LOADED);
        y += 22;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.LOAD_SCHEMATICS);
        y += 44;

        this.createChangeMenuButton(x, y, ButtonListenerChangeMenu.ButtonType.SHOW_AREA_SELECTIONS);
    }

    private void createChangeMenuButton(int x, int y, ButtonListenerChangeMenu.ButtonType type)
    {
        ButtonGeneric button = new ButtonGeneric(this.id++, x, y, 200, 20, I18n.format(type.getLabelKey()), type.getIcon());
        this.addButton(button, new ButtonListenerChangeMenu(type));
    }

    public static class ButtonListenerChangeMenu implements IButtonActionListener<ButtonGeneric>
    {
        private final ButtonType type;

        public ButtonListenerChangeMenu(ButtonType type)
        {
            this.type = type;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.SHOW_LOADED)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiLoadedSchematicsManager());
            }
            else if (this.type == ButtonType.SHOW_PLACEMENTS)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiPlacementManager());
            }
            else if (this.type == ButtonType.SHOW_AREA_SELECTIONS)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiAreaSelectionManager());
            }
            else if (this.type == ButtonType.LOAD_SCHEMATICS)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiSchematicLoad());
            }
            else if (this.type == ButtonType.MAIN_MENU)
            {
                Minecraft.getMinecraft().displayGuiScreen(new GuiMainMenu());
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
            SHOW_LOADED             ("litematica.gui.button.change_menu.show_loaded_schematics", ButtonIcon.LOADED_SCHEMATICS),
            // List Schematics placements
            SHOW_PLACEMENTS         ("litematica.gui.button.change_menu.show_schematic_placements", ButtonIcon.SCHEMATIC_PLACEMENTS),
            // Load Schematics from file to memory
            SHOW_AREA_SELECTIONS    ("litematica.gui.button.change_menu.show_area_selections", ButtonIcon.AREA_SELECTION),
            // Load Schematics from file to memory
            LOAD_SCHEMATICS         ("litematica.gui.button.change_menu.load_schematics_to_memory", ButtonIcon.SCHEMATIC_BROWSER),
            // Create a new Schematic from an area selection
            CREATE_SCHEMATIC        ("litematica.gui.button.change_menu.create_schematic_from_area", null),
            // Edit Schematics (description or icon), or convert between formats
            MANAGE_SCHEMATICS       ("litematica.gui.button.change_menu.manage_schematics", null),
            // Switch to the Litematica main menu
            MAIN_MENU               ("litematica.gui.button.change_menu.to_main_menu", null);

            private final String labelKey;
            private final ButtonIcon icon;

            private ButtonType(String labelKey, ButtonIcon icon)
            {
                this.labelKey = labelKey;
                this.icon = icon;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }

            public ButtonIcon getIcon()
            {
                return this.icon;
            }
        }
    }
}
