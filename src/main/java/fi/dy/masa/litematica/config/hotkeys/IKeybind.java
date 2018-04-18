package fi.dy.masa.litematica.config.hotkeys;

import java.util.Collection;
import javax.annotation.Nullable;

public interface IKeybind
{
    void setCallback(@Nullable IHotkeyCallback callback);

    boolean isValid();

    boolean isPressed();

    boolean isKeybindHeld(boolean checkNow);

    void clearKeys();

    void addKey(int keyCode);

    void removeKey(int keyCode);

    void tick();

    String getKeysDisplayString();

    String getStorageString();

    void setKeysFromStorageString(String key);

    Collection<Integer> getKeys();
}
