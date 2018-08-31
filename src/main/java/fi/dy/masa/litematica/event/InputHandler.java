package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler, IMouseInputHandler
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, "litematica.hotkeys.category.generic_hotkeys", Hotkeys.HOTKEY_LIST);
    }

    @Override
    public boolean onKeyInput(int eventKey, boolean eventKeyState)
    {
        if (eventKeyState && Minecraft.getMinecraft().gameSettings.keyBindScreenshot.getKeyCode() == eventKey)
        {
            return GuiSchematicManager.setPreviewImage();
        }

        return false;
    }

    @Override
    public boolean onMouseInput(int eventButton, int dWheel, boolean eventButtonState)
    {
        Minecraft mc = Minecraft.getMinecraft();

        // Not in a GUI
        if (mc.currentScreen == null && mc.world != null && mc.player != null)
        {
            EntityPlayer player = mc.player;
            final boolean hasTool = EntityUtils.isHoldingItem(player, DataManager.getToolItem());

            if (hasTool == false)
            {
                return false;
            }

            OperationMode mode = DataManager.getOperationMode();

            if (dWheel != 0)
            {
                final int amount = dWheel > 0 ? 1 : -1;

                if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = DataManager.getInstance().getSelectionManager();

                        if (sm.hasGrabbedElement())
                        {
                            sm.changeGrabDistance(player, amount);
                            return true;
                        }
                        else if (sm.hasSelectedElement() || (sm.getCurrentSelection() != null && sm.getCurrentSelection().isOriginSelected()))
                        {
                            sm.moveSelectedElement(EntityUtils.getClosestLookingDirection(player), amount);
                            return true;
                        }
                    }
                    else if (mode.getUsesSchematic())
                    {
                        EnumFacing direction = EntityUtils.getClosestLookingDirection(player);
                        DataManager.getInstance().getSchematicPlacementManager().nudgePositionOfCurrentSelection(direction, amount);
                        return true;
                    }
                }
                else if (Hotkeys.OPERATION_MODE_CHANGE_MODIFIER.getKeybind().isKeybindHeld())
                {
                    DataManager.setOperationMode(DataManager.getOperationMode().cycle(player, amount < 0));
                    return true;
                }
            }
        }

        return false;
    }

    public static void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getInstance().getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }
        }
    }
}
