package fi.dy.masa.litematica.schematic;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;

public class SchematicHolder
{
    private static final SchematicHolder INSTANCE = new SchematicHolder();
    private final Map<Integer, LitematicaSchematic> schematics = new TreeMap<>();
    private int nextId;

    public static SchematicHolder getInstance()
    {
        return INSTANCE;
    }

    /**
     * Adds the given schematic
     * @param schematic
     * @return the index at which
     */
    public int addSchematic(LitematicaSchematic schematic)
    {
        int id = this.nextId++;
        this.schematics.put(id, schematic);
        return id;
    }

    @Nullable
    public LitematicaSchematic getSchematic(int id)
    {
        return this.schematics.get(id);
    }

    public boolean removeSchematic(int id)
    {
        return this.schematics.remove(id) != null;
    }

    public Collection<LitematicaSchematic> getAllSchematics()
    {
        return this.schematics.values();
    }
}
