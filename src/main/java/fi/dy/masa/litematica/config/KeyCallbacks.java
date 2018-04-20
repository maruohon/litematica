package fi.dy.masa.litematica.config;

import fi.dy.masa.litematica.config.hotkeys.IHotkeyCallback;
import fi.dy.masa.litematica.config.hotkeys.IKeybind;
import fi.dy.masa.litematica.config.hotkeys.KeyAction;
import fi.dy.masa.litematica.gui.GuiSelectionManager;
import net.minecraft.client.Minecraft;

public class KeyCallbacks
{
    public static void init()
    {
        Minecraft mc = Minecraft.getMinecraft();

        IHotkeyCallback callback = new KeyCallbackHotkeys(mc);
        Hotkeys.OPEN_GUI_SELECTION_MANAGER.getKeybind().setCallback(callback);
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
                    this.mc.displayGuiScreen(new GuiSelectionManager());
                }
            }
        }
    }
}
