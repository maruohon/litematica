package fi.dy.masa.litematica.config.interfaces;

import javax.annotation.Nullable;

public interface INamed
{
    String getName();

    @Nullable
    String getComment();
}
