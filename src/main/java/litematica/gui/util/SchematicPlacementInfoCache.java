package litematica.gui.util;

import java.nio.file.Path;
import java.util.HashMap;
import javax.annotation.Nullable;

import litematica.schematic.placement.SchematicPlacementUnloaded;

public class SchematicPlacementInfoCache
{
    protected final HashMap<Path, SchematicPlacementUnloaded> cachedData = new HashMap<>();

    @Nullable
    public SchematicPlacementUnloaded getPlacementInfo(Path file)
    {
        return this.cachedData.get(file);
    }

    @Nullable
    public SchematicPlacementUnloaded cacheAndGetPlacementInfo(Path file)
    {
        if (this.cachedData.containsKey(file) == false)
        {
            this.cachedData.put(file, SchematicPlacementUnloaded.fromFile(file));
        }

        return this.cachedData.get(file);
    }
}
