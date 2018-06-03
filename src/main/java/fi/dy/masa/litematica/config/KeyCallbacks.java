package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.hotkeys.IHotkeyCallback;
import fi.dy.masa.litematica.config.hotkeys.IKeybind;
import fi.dy.masa.litematica.config.hotkeys.KeyAction;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.data.Placement;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.event.RenderEventHandler;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.gui.GuiMainMenu;
import fi.dy.masa.litematica.gui.GuiPlacementConfiguration;
import fi.dy.masa.litematica.gui.GuiSubRegionConfiguration;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class KeyCallbacks
{
    public static void init()
    {
        Minecraft mc = Minecraft.getMinecraft();

        IHotkeyCallback callbackGeneric = new KeyCallbackHotkeys(mc);
        IHotkeyCallback callbackMessage = new KeyCallbackToggleMessage(mc);

        Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind().setCallback(callbackGeneric);
        Hotkeys.OPEN_GUI_SCHEMATIC_ACTIONS.getKeybind().setCallback(callbackGeneric);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callbackGeneric);

        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind().setCallback(callbackMessage);
    }

    private static class KeyCallbackHotkeys implements IHotkeyCallback
    {
        private final Minecraft mc;

        public KeyCallbackHotkeys(Minecraft mc)
        {
            this.mc = mc;
        }

        @Override
        public void onKeyAction(KeyAction action, IKeybind key)
        {
            if (action == KeyAction.PRESS)
            {
                if (key == Hotkeys.OPEN_GUI_PLACEMENT_SETTINGS.getKeybind())
                {
                    SchematicPlacement schematicPlacement = DataManager.getInstance(this.mc.world).getSchematicPlacementManager().getSelectedSchematicPlacement();

                    if (schematicPlacement != null)
                    {
                        Placement placement = schematicPlacement.getSelectedSubRegionPlacement();

                        if (placement != null)
                        {
                            this.mc.displayGuiScreen(new GuiSubRegionConfiguration(schematicPlacement, placement));
                        }
                        else
                        {
                            this.mc.displayGuiScreen(new GuiPlacementConfiguration(schematicPlacement));
                        }
                    }
                }
                else if (key == Hotkeys.OPEN_GUI_SCHEMATIC_ACTIONS.getKeybind())
                {
                    this.mc.displayGuiScreen(new GuiMainMenu());
                }
                else if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
                {
                    this.mc.displayGuiScreen(new GuiAreaSelectionManager());
                }
            }
        }
    }

    private static class KeyCallbackToggleMessage implements IHotkeyCallback
    {
        private final Minecraft mc;

        public KeyCallbackToggleMessage(Minecraft mc)
        {
            this.mc = mc;
        }

        @Override
        public void onKeyAction(KeyAction action, IKeybind key)
        {
            if (action == KeyAction.PRESS)
            {
                if (key == Hotkeys.ADD_SELECTION_BOX.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.createNewSubRegionBox(pos);

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.added_selection_box", posStr);
                    }
                }
                else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        String name = selection.getCurrentSubRegionBoxName();

                        if (name != null && selection.removeSelectedSubRegionBox())
                        {
                            KeyCallbacks.printMessage(this.mc, "litematica.message.removed_selection_box", name);
                        }
                    }
                }
                else if (key == Hotkeys.MOVE_ENTIRE_SELECTION.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        BlockPos old = selection.getOrigin();
                        selection.moveEntireSelectionTo(pos);
                        String oldStr = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.moved_selection", oldStr, posStr);
                    }
                }
                else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.setOrigin(pos);
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.set_area_origin", posStr);
                    }
                }
                else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() || key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
                {
                    SelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection selection = sm.getCurrentSelection();

                    if (selection != null && selection.getSelectedSubRegionBox() != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        int p = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? 1 : 2;

                        if (p == 1)
                        {
                            selection.getSelectedSubRegionBox().setPos1(pos);
                        }
                        else
                        {
                            selection.getSelectedSubRegionBox().setPos2(pos);
                        }

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.set_selection_box_point", p, posStr);
                    }
                }
                else if (key == Hotkeys.TOGGLE_ALL_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleAllRenderingEnabled();
                    String name = splitCamelCase(Hotkeys.TOGGLE_ALL_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                }
                else if (key == Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleRenderSelectionBoxes();
                    String name = splitCamelCase(Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                }
                else if (key == Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getKeybind())
                {
                    boolean enabled = RenderEventHandler.getInstance().toggleRenderSchematics();
                    String name = splitCamelCase(Hotkeys.TOGGLE_GHOST_BLOCK_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                }
                else if (key == Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getKeybind())
                {
                    boolean enabled = ! Configs.Generic.RENDER_AS_TRANSLUCENT.getValue();
                    Configs.Generic.RENDER_AS_TRANSLUCENT.setValue(enabled);
                    String name = splitCamelCase(Hotkeys.TOGGLE_TRANSLUCENT_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                }
            }
        }

        protected void printToggleMessage(String name, boolean enabled)
        {
            // FIXME
            String pre = enabled ? TextFormatting.GREEN.toString() : TextFormatting.RED.toString();
            String status = I18n.format("litematica.message.value." + (enabled ? "on" : "off"));
            String message = I18n.format("litematica.message.toggled", name, pre + status + TextFormatting.RESET);
            KeyCallbacks.printMessage(this.mc, message);
        }
    }

    public static void printMessage(Minecraft mc, String key, Object... args)
    {
        mc.ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentTranslation(key, args));
    }

    // https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
    public static String splitCamelCase(String str)
    {
        return str.replaceAll(
           String.format("%s|%s|%s",
              "(?<=[A-Z])(?=[A-Z][a-z])",
              "(?<=[^A-Z])(?=[A-Z])",
              "(?<=[A-Za-z])(?=[^A-Za-z])"
           ),
           " "
        );
     }
}
