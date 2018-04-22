package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.hotkeys.IHotkeyCallback;
import fi.dy.masa.litematica.config.hotkeys.IKeybind;
import fi.dy.masa.litematica.config.hotkeys.KeyAction;
import fi.dy.masa.litematica.event.RenderEventHandler;
import fi.dy.masa.litematica.gui.GuiAreaSelectionManager;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.util.AreaSelectionManager;
import fi.dy.masa.litematica.util.DataManager;
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

        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callbackGeneric);

        Hotkeys.ADD_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.DELETE_SELECTION_BOX.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_AREA_ORIGIN.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind().setCallback(callbackMessage);
        Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_ALL_RENDERING.getKeybind().setCallback(callbackMessage);
        Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getKeybind().setCallback(callbackMessage);
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
                if (key == Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind())
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
                    AreaSelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection sel = sm.getSelectedAreaSelection();

                    if (sel != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        sel.createNewSelectionBox(pos);

                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.added_selection_box", posStr);
                    }
                }
                else if (key == Hotkeys.DELETE_SELECTION_BOX.getKeybind())
                {
                    AreaSelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection sel = sm.getSelectedAreaSelection();

                    if (sel != null)
                    {
                        String name = sel.getCurrentSelectionBoxName();

                        if (sel.removeSelectedSelectionBox())
                        {
                            KeyCallbacks.printMessage(this.mc, "litematica.message.removed_selection_box", name);
                        }
                    }
                }
                else if (key == Hotkeys.SET_AREA_ORIGIN.getKeybind())
                {
                    AreaSelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection area = sm.getSelectedAreaSelection();

                    if (area != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        area.setOrigin(pos);
                        String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                        KeyCallbacks.printMessage(this.mc, "litematica.message.set_area_origin", posStr);
                    }
                }
                else if (key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() || key == Hotkeys.SET_SELECTION_BOX_POSITION_2.getKeybind())
                {
                    AreaSelectionManager sm = DataManager.getInstance(this.mc.world).getSelectionManager();
                    AreaSelection sel = sm.getSelectedAreaSelection();

                    if (sel != null && sel.getSelectedSelectionBox() != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        int p = key == Hotkeys.SET_SELECTION_BOX_POSITION_1.getKeybind() ? 1 : 2;

                        if (p == 1)
                        {
                            sel.getSelectedSelectionBox().setPos1(pos);
                        }
                        else
                        {
                            sel.getSelectedSelectionBox().setPos2(pos);
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
                    boolean enabled = RenderEventHandler.getInstance().toggleSelectionBoxesRenderingEnabled();
                    String name = splitCamelCase(Hotkeys.TOGGLE_SELECTION_BOXES_RENDERING.getName());
                    this.printToggleMessage(name, enabled);
                }
            }
        }

        protected void printToggleMessage(String name, boolean enabled)
        {
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
