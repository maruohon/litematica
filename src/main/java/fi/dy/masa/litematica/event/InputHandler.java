package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

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
        if (eventKeyState)
        {
            Minecraft mc = Minecraft.getMinecraft();

            if (eventKey == mc.gameSettings.keyBindUseItem.getKeyCode())
            {
                return this.handleUseKey(mc);
            }
            else if (eventKey == mc.gameSettings.keyBindScreenshot.getKeyCode())
            {
                return GuiSchematicManager.setPreviewImage();
            }
        }

        return false;
    }

    @Override
    public boolean onMouseInput(int eventButton, int dWheel, boolean eventButtonState)
    {
        Minecraft mc = Minecraft.getMinecraft();
        boolean toolEnabled = Configs.Visuals.ENABLE_RENDERING.getBooleanValue() && Configs.Generic.TOOL_ITEM_ENABLED.getBooleanValue();

        // Tool enabled, and not in a GUI
        if (mc.currentScreen == null && mc.world != null && mc.player != null)
        {
            if (eventButtonState && eventButton == mc.gameSettings.keyBindUseItem.getKeyCode() + 100)
            {
                return this.handleUseKey(mc);
            }

            EntityPlayer player = mc.player;

            if (toolEnabled == false || EntityUtils.isHoldingItem(player, DataManager.getToolItem()) == false)
            {
                return false;
            }

            OperationMode mode = DataManager.getOperationMode();

            if (dWheel != 0)
            {
                final int amount = dWheel > 0 ? 1 : -1;

                if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
                {
                    if (mode.getUsesAreaSelection())
                    {
                        SelectionManager sm = DataManager.getSelectionManager();

                        if (sm.hasGrabbedElement())
                        {
                            sm.changeGrabDistance(player, amount);
                            return true;
                        }
                        else if (sm.hasSelectedOrigin())
                        {
                            AreaSelection area = sm.getCurrentSelection();
                            BlockPos old = area.getEffectiveOrigin();
                            area.moveEntireSelectionTo(old.offset(EntityUtils.getClosestLookingDirection(player), amount), false);
                            return true;
                        }
                    }
                }
                else if (Hotkeys.SELECTION_NUDGE_MODIFIER.getKeybind().isKeybindHeld())
                {
                    return nudgeSelection(amount, mode, player);
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

    public static boolean nudgeSelection(int amount, OperationMode mode, EntityPlayer player)
    {
        if (mode.getUsesAreaSelection())
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasSelectedElement())
            {
                sm.moveSelectedElement(EntityUtils.getClosestLookingDirection(player), amount);
                return true;
            }
        }
        else if (mode.getUsesSchematic())
        {
            EnumFacing direction = EntityUtils.getClosestLookingDirection(player);
            DataManager.getSchematicPlacementManager().nudgePositionOfCurrentSelection(direction, amount);
            return true;
        }

        return false;
    }

    private boolean handleUseKey(Minecraft mc)
    {
        if (mc.player != null)
        {
            if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
                (Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() ||
                 Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isValid() == false))
            {
                WorldUtils.handleEasyPlace(mc);
                return true;
            }
            else if (Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue())
            {
                String keyStrUse = KeybindMulti.getStorageStringForKeyCode(mc.gameSettings.keyBindUseItem.getKeyCode());
                String keyStrPick = Hotkeys.PICK_BLOCK_LAST.getKeybind().getStringValue();

                if (keyStrUse.equals(keyStrPick))
                {
                    WorldUtils.doSchematicWorldPickBlock(false, mc);
                }
            }

            if (Configs.Generic.PLACEMENT_RESTRICTION.getBooleanValue())
            {
                return WorldUtils.handlePlacementRestriction(mc);
            }
        }

        return false;
    }

    public static void onTick()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }
        }
    }
}
