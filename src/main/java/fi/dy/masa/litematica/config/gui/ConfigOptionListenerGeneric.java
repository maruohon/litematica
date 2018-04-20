package fi.dy.masa.litematica.config.gui;

import com.mumfrey.liteloader.modconfig.AbstractConfigPanel.ConfigOptionListener;
import fi.dy.masa.litematica.config.gui.button.ConfigButtonBase;

public class ConfigOptionListenerGeneric<T extends ConfigButtonBase> implements ConfigOptionListener<T>
{
    private boolean dirty;

    @Override
    public void actionPerformed(T control)
    {
        this.dirty = true;
    }

    public boolean isDirty()
    {
        return this.dirty;
    }

    public void resetDirty()
    {
        this.dirty = false;
    }
}
