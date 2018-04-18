package fi.dy.masa.litematica.config.hotkeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.lwjgl.input.Keyboard;
import com.google.common.collect.ImmutableList;

public class KeybindMulti implements IKeybind
{
    private List<Integer> keyCodes = new ArrayList<>(4);
    private boolean pressed;
    private int heldTime;
    @Nullable
    private IHotkeyCallback callback;

    @Override
    public void setCallback(@Nullable IHotkeyCallback callback)
    {
        this.callback = callback;
    }

    @Override
    public boolean isValid()
    {
        return this.keyCodes.isEmpty() == false;
    }

    /**
     * Checks if this keybind is now active but previously was not active,
     * and then updates the cached state.
     * @return true if this keybind just became pressed
     */
    @Override
    public boolean isPressed()
    {
        if (this.isValid())
        {
            this.updateIsPressed();
            return this.pressed && this.heldTime == 0;
        }
        else
        {
            this.pressed = false;
            return false;
        }
    }

    /**
     * Returns whether the keybind is being held down.
     * @return
     */
    @Override
    public boolean isKeybindHeld(boolean checkNow)
    {
        if (checkNow && this.isValid())
        {
            this.updateIsPressed();
        }

        return this.pressed;
    }

    private void updateIsPressed()
    {
        int activeCount = 0;

        for (Integer keyCode : this.keyCodes)
        {
            if (Keyboard.isKeyDown(keyCode))
            {
                activeCount++;
            }
        }

        boolean pressedLast = this.pressed;
        this.pressed = activeCount == this.keyCodes.size();

        if (this.pressed == false)
        {
            this.heldTime = 0;

            if (pressedLast && this.callback != null)
            {
                this.callback.onKeyAction(KeyAction.RELEASE, this);
            }
        }
        else if (this.heldTime == 0 && this.callback != null)
        {
            this.callback.onKeyAction(KeyAction.PRESS, this);
        }
    }

    @Override
    public void clearKeys()
    {
        this.keyCodes.clear();
        this.pressed = false;
        this.heldTime = 0;
    }

    @Override
    public void addKey(int keyCode)
    {
        if (this.keyCodes.contains(keyCode) == false)
        {
            this.keyCodes.add(keyCode);
        }
    }

    @Override
    public void tick()
    {
        if (this.pressed)
        {
            this.heldTime++;
        }
    }

    @Override
    public void removeKey(int keyCode)
    {
        this.keyCodes.remove(keyCode);
    }

    @Override
    public Collection<Integer> getKeys()
    {
        return ImmutableList.copyOf(this.keyCodes);
    }

    @Override
    public String getKeysDisplayString()
    {
        return this.getStorageString().replaceAll(",", " + ");
    }

    @Override
    public String getStorageString()
    {
        StringBuilder sb = new StringBuilder(16);
        int i = 0;

        for (Integer keyCode : this.keyCodes)
        {
            if (i > 0)
            {
                sb.append(",");
            }

            sb.append(Keyboard.getKeyName(keyCode));
            i++;
        }

        return sb.toString();
    }

    @Override
    public void setKeysFromStorageString(String str)
    {
        this.clearKeys();
        String[] keys = str.split(",");

        for (String key : keys)
        {
            key = key.trim();

            if (key.isEmpty() == false)
            {
                int keyCode = Keyboard.getKeyIndex(key);

                if (keyCode != Keyboard.KEY_NONE)
                {
                    this.keyCodes.add(keyCode);
                }
            }
        }
    }

    public static KeybindMulti fromStorageString(String str)
    {
        KeybindMulti keybind = new KeybindMulti();
        keybind.setKeysFromStorageString(str);
        return keybind;
    }
}
