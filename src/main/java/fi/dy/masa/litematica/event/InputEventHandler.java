package fi.dy.masa.litematica.event;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.OperationMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.malilib.hotkeys.IKeyEventHandler;
import fi.dy.masa.malilib.hotkeys.IMouseEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class InputEventHandler implements IKeyEventHandler, IMouseEventHandler
{
    private static final InputEventHandler INSTANCE = new InputEventHandler();
    private final Set<Integer> genericHotkeysUsedKeys = new HashSet<>();
    private final Set<Integer> modifierKeys = new HashSet<>();
    private final Minecraft mc;
    public static Box selection = new Box(); // FIXME testing stuff

    private InputEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
        this.modifierKeys.add(Keyboard.KEY_LSHIFT);
        this.modifierKeys.add(Keyboard.KEY_RSHIFT);
        this.modifierKeys.add(Keyboard.KEY_LCONTROL);
        this.modifierKeys.add(Keyboard.KEY_RCONTROL);
        this.modifierKeys.add(Keyboard.KEY_LMENU);
        this.modifierKeys.add(Keyboard.KEY_RMENU);
    }

    public static InputEventHandler getInstance()
    {
        return INSTANCE;
    }

    public void updateUsedKeys()
    {
        this.genericHotkeysUsedKeys.clear();

        for (Hotkeys hotkey : Hotkeys.values())
        {
            this.genericHotkeysUsedKeys.addAll(hotkey.getKeybind().getKeys());
        }
    }

    @Override
    public boolean onKeyInput(int eventKey, boolean eventKeyState)
    {
        // Not in a GUI
        if (this.mc.currentScreen == null)
        {
            boolean cancel = false;

            if (this.genericHotkeysUsedKeys.contains(eventKey))
            {
                for (Hotkeys hotkey : Hotkeys.values())
                {
                    // Note: isPressed() has to get called for key releases too, to reset the state
                    cancel |= hotkey.getKeybind().isPressed();
                }
            }

            // FIXME temporary testing stuff
            if (Keyboard.getEventKeyState() && GuiScreen.isAltKeyDown())
            {
                if (eventKey == Keyboard.KEY_0)
                {
                    DataManager.save();
                    this.mc.displayGuiScreen(new GuiSchematicSave());
                    return true;
                }
                if (eventKey == Keyboard.KEY_3)
                {
                    File file = new File(new File(this.mc.mcDataDir, "schematics"), "test.schematic");
                    SchematicaSchematic schematic = SchematicaSchematic.createFromFile(file);
                    RenderEventHandler.getInstance().setSchematic(schematic);

                    if (schematic != null)
                    {
                        BlockPos pos = new BlockPos(this.mc.player.getPositionVector());
                        selection.setPos1(pos);
                        selection.setPos2(pos.add(schematic.getSize()).add(-1, -1, -1));
                        BlockPos s = schematic.getSize();
                        this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO,
                                new TextComponentString(String.format("Schematic loaded, size: %d x %d x %d", s.getX(), s.getY(), s.getZ())));
                        return true;
                    }
                }
            }

            // Somewhat hacky fix to prevent eating the modifier keys... >_>
            // A proper fix would likely require adding a context for the keys,
            // and only cancel if the context is currently active/valid.
            return cancel && this.modifierKeys.contains(eventKey) == false;
        }

        return false;
    }

    @Override
    public boolean onMouseInput(int eventButton, int dWheel, boolean eventButtonState)
    {
        // Not in a GUI
        if (this.mc.currentScreen == null && this.mc.world != null && this.mc.player != null)
        {
            World world = this.mc.world;
            EntityPlayer player = this.mc.player;
            final boolean hasTool = EntityUtils.isHoldingItem(player, DataManager.getToolItem());
            OperationMode mode = DataManager.getOperationMode();

            if (hasTool == false)
            {
                return false;
            }

            if (dWheel != 0)
            {
                final int amount = dWheel > 0 ? 1 : -1;

                if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = DataManager.getInstance(world).getSelectionManager();

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
                    else if (mode == OperationMode.PLACEMENT)
                    {
                        EnumFacing direction = EntityUtils.getClosestLookingDirection(player);
                        DataManager.getInstance(world).getSchematicPlacementManager().nudgePositionOfCurrentSelection(direction, amount);
                        return true;
                    }
                }
                else if (Hotkeys.OPERATION_MODE_CHANGE_MODIFIER.getKeybind().isKeybindHeld())
                {
                    DataManager.setOperationMode(DataManager.getOperationMode().cycle(amount < 0));
                    return true;
                }
            }
            else if (eventButtonState && RenderEventHandler.getInstance().isEnabled())
            {
                boolean isLeftClick = mouseEventIsAttack(this.mc, eventButton);
                boolean isRightClick = mouseEventIsUse(this.mc, eventButton);
                int maxDistance = 200;

                if (isLeftClick || isRightClick)
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = DataManager.getInstance(world).getSelectionManager();
                        sm.setPositionOfCurrentSelectionToRayTrace(this.mc, isLeftClick ? Corner.CORNER_1 : Corner.CORNER_2, maxDistance);
                        return true;
                    }
                    else if (mode == OperationMode.PLACEMENT)
                    {
                        DataManager.getInstance(world).getSchematicPlacementManager().setPositionOfCurrentSelectionToRayTrace(this.mc, maxDistance);
                        return true;
                    }
                }
                else if (mouseEventIsPickBlock(this.mc, eventButton))
                {
                    if (mode == OperationMode.AREA_SELECTION)
                    {
                        SelectionManager sm = DataManager.getInstance(world).getSelectionManager();

                        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld())
                        {
                            if (sm.hasGrabbedElement())
                            {
                                sm.releaseGrabbedElement();
                                return true;
                            }
                            else
                            {
                                sm.grabElement(this.mc, maxDistance);
                                return true;
                            }
                        }
                        else
                        {
                            sm.changeSelection(world, player, maxDistance);
                            return true;
                        }
                    }
                    else if (mode == OperationMode.PLACEMENT)
                    {
                        DataManager.getInstance(world).getSchematicPlacementManager().changeSelection(world, player, maxDistance);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static void onTick()
    {
        for (Hotkeys hotkey : Hotkeys.values())
        {
            hotkey.getKeybind().tick();
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getInstance(mc.world).getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }
        }
    }

    public static boolean mouseEventIsAttack(Minecraft mc, int button)
    {
        return button == mc.gameSettings.keyBindAttack.getKeyCode() + 100;
    }

    public static boolean mouseEventIsUse(Minecraft mc, int button)
    {
        return button == mc.gameSettings.keyBindUseItem.getKeyCode() + 100;
    }

    public static boolean mouseEventIsPickBlock(Minecraft mc, int button)
    {
        return button == mc.gameSettings.keyBindPickBlock.getKeyCode() + 100;
    }
}
