package fi.dy.masa.litematica.render;

import java.util.List;

public interface IStringListProvider
{
    boolean shouldRenderStrings();

    List<String> getLines();
}
