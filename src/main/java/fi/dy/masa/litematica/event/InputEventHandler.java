package fi.dy.masa.litematica.event;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.config.KeyCallbacks;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.Selection;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class InputEventHandler
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

    public boolean onKeyInput()
    {
        // Not in a GUI
        if (this.mc.currentScreen == null)
        {
            final int eventKey = Keyboard.getEventKey();
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

    public boolean onMouseInput()
    {
        // Not in a GUI
        if (this.mc.currentScreen == null && this.mc.world != null && this.mc.player != null)
        {
            World world = this.mc.world;
            EntityPlayer player = this.mc.player;
            final int dWheel = Mouse.getEventDWheel() / 120;
            final boolean hasTool = EntityUtils.isHoldingItem(player, DataManager.toolItem.getItem());

            if (dWheel != 0)
            {
                if (hasTool && Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld(false))
                {
                    SelectionManager sm = DataManager.getInstance(world).getSelectionManager();

                    if (sm.hasGrabbedElement())
                    {
                        sm.changeGrabDistance(player, dWheel);
                        return true;
                    }
                    else if (sm.hasSelectedElement())
                    {
                        sm.moveSelectedElement(EntityUtils.getClosestLookingDirection(player), dWheel);
                        return true;
                    }
                }
            }
            else if (Mouse.getEventButtonState())
            {
                final int button = Mouse.getEventButton();

                if (mouseEventIsPickBlock(this.mc, button))
                {
                    // TODO: if (isInSelectionMode())
                    {
                        SelectionManager sm = DataManager.getInstance(world).getSelectionManager();

                        if (Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld(false))
                        {
                            if (sm.hasGrabbedElement())
                            {
                                sm.releaseGrabbedElement();
                            }
                            else
                            {
                                sm.grabElement(this.mc, 200);
                            }
                        }
                        else
                        {
                            sm.changeSelection(world, player, 200);
                        }

                        return true;
                    }
                }
                else if (mouseEventIsAttack(this.mc, button))
                {
                    if (hasTool && Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld(false))
                    {
                        this.setCornerPosition(this.mc, Corner.CORNER_1);
                    }
                }
                else if (mouseEventIsUse(this.mc, button))
                {
                    if (hasTool && Hotkeys.SELECTION_GRAB_MODIFIER.getKeybind().isKeybindHeld(false))
                    {
                        this.setCornerPosition(this.mc, Corner.CORNER_2);
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

    private void setCornerPosition(Minecraft mc, Corner corner)
    {
        SelectionManager sm = DataManager.getInstance(mc.world).getSelectionManager();
        Selection sel = sm.getCurrentSelection();

        if (corner != Corner.NONE && sel != null && sel.getSelectedSelectionBox() != null)
        {
            RayTraceResult trace = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, 200);

            if (trace.typeOfHit != RayTraceResult.Type.BLOCK)
            {
                return;
            }

            int cornerIndex = 1;
            BlockPos pos = trace.getBlockPos();

            // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
            if (mc.player.isSneaking() == false)
            {
                pos = pos.offset(trace.sideHit);
            }

            if (corner == Corner.CORNER_1)
            {
                sel.getSelectedSelectionBox().setPos1(pos);
            }
            else if (corner == Corner.CORNER_2)
            {
                sel.getSelectedSelectionBox().setPos2(pos);
                cornerIndex = 2;
            }

            String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
            KeyCallbacks.printMessage(this.mc, "litematica.message.set_selection_box_point", cornerIndex, posStr);
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
