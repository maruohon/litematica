package fi.dy.masa.litematica.event;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.util.Selection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class InputEventHandler
{
    private static final InputEventHandler INSTANCE = new InputEventHandler();
    private final Set<Integer> genericHotkeysUsedKeys = new HashSet<>();
    private final Set<Integer> modifierKeys = new HashSet<>();
    private final Minecraft mc;
    public static Selection selection = new Selection(); // FIXME testing stuff

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

        /*
        for (Hotkeys hotkey : Hotkeys.values())
        {
            this.genericHotkeysUsedKeys.addAll(hotkey.getKeybind().getKeys());
        }
        */
    }

    public boolean onKeyInput()
    {
        // Not in a GUI
        if (this.mc.currentScreen == null)
        {
            final int eventKey = Keyboard.getEventKey();
            boolean cancel = false;

            /*
            if (this.genericHotkeysUsedKeys.contains(eventKey))
            {
                for (Hotkeys hotkey : Hotkeys.values())
                {
                    // Note: isPressed() has to get called for key releases too, to reset the state
                    cancel |= hotkey.getKeybind().isPressed();
                }
            }
            */
            // FIXME temporary testing stuff
            if (Keyboard.getEventKeyState() && GuiScreen.isAltKeyDown())
            {
                if (eventKey == Keyboard.KEY_0)
                {
                    RenderEventHandler.getInstance().toggleEnabled();
                    String str = RenderEventHandler.getInstance().isEnabled() ? TextFormatting.GREEN + "enabled" : TextFormatting.RED + "disabled";
                    this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentString("Rendering " + str));
                }
                else if (eventKey == Keyboard.KEY_1)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());

                    if (selection.getPos1() != null && selection.getPos1().equals(pos))
                    {
                        selection.setPos1(null);
                    }
                    else
                    {
                        selection.setPos1(pos);
                        this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO,
                                new TextComponentString(String.format("Set pos1 to x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ())));
                    }
                }
                else if (eventKey == Keyboard.KEY_2)
                {
                    BlockPos pos = new BlockPos(this.mc.player.getPositionVector());

                    if (selection.getPos2() != null && selection.getPos2().equals(pos))
                    {
                        selection.setPos2(null);
                    }
                    else
                    {
                        selection.setPos2(pos);
                        this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO,
                                new TextComponentString(String.format("Set pos2 to x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ())));
                    }
                }
                else if (eventKey == Keyboard.KEY_3)
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
                    }
                }

                return true;
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
        if (this.mc.currentScreen == null)
        {
            
        }

        return false;
    }
}
