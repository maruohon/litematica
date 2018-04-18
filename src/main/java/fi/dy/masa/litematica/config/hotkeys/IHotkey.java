package fi.dy.masa.litematica.config.hotkeys;

import fi.dy.masa.litematica.config.interfaces.INamed;

public interface IHotkey extends INamed
{
    IKeybind getKeybind();

    void setKeybind(IKeybind keybind);
}
