package fi.dy.masa.litematica.gui.util;

import java.io.File;
import java.util.HashMap;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;

public class SchematicPlacementInfoCache
{
    protected final HashMap<File, SchematicPlacementUnloaded> cachedData = new HashMap<>();

    @Nullable
    public SchematicPlacementUnloaded getPlacementInfo(File file)
    {
        return this.cachedData.get(file);
    }

    @Nullable
    public SchematicPlacementUnloaded cacheAndGetPlacementInfo(File file)
    {
        if (this.cachedData.containsKey(file) == false)
        {
            this.cachedData.put(file, SchematicPlacementUnloaded.fromFile(file));
        }

        return this.cachedData.get(file);
    }
}
