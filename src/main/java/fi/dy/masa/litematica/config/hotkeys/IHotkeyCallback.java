package fi.dy.masa.litematica.config.hotkeys;

public interface IHotkeyCallback
{
    /**
     * Called when a hotkey action happens.
     * @param action
     * @param key
     */
    void onKeyAction(KeyAction action, IKeybind key);
}
