package fi.dy.masa.litematica.gui.interfaces;

import javax.annotation.Nullable;

public interface ISelectionListener<T>
{
    void onSelectionChange(@Nullable T entry);
}
