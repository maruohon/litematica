package fi.dy.masa.litematica.config.gui.button;

import com.mumfrey.liteloader.modconfig.AbstractConfigPanel.ConfigOptionListener;

public interface IButtonActionListener<T extends ButtonBase> extends ConfigOptionListener<T>
{
    void actionPerformedWithButton(T control, int mouseButton);
}
