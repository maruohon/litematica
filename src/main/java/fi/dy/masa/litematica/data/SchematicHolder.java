package fi.dy.masa.litematica.data;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.client.Minecraft;

public class SchematicHolder
{
    private static final SchematicHolder INSTANCE = new SchematicHolder();
    private final Map<Integer, SchematicEntry> schematics = new TreeMap<>();
    private int nextId;

    public static SchematicHolder getInstance()
    {
        return INSTANCE;
    }

    public void clearLoadedSchematics()
    {
        this.schematics.clear();
        this.nextId = 0;
    }

    /**
     * Adds the given schematic
     * @param schematic
     * @return the index at which
     */
    public int addSchematic(LitematicaSchematic schematic, String schematicName, @Nullable String fileName)
    {
        int id = this.nextId++;
        this.schematics.put(id, new SchematicEntry(schematic, id, schematicName, fileName));
        return id;
    }

    @Nullable
    public SchematicEntry getSchematic(int id)
    {
        return this.schematics.get(id);
    }

    public boolean removeSchematic(int id)
    {
        SchematicEntry entry = this.schematics.remove(id);

        if (entry != null)
        {
            Minecraft mc = Minecraft.getMinecraft();
            int dimension = mc.world.provider.getDimensionType().getId();
            DataManager.getInstance(dimension).getSchematicPlacementManager().removeAllPlacementsOfSchematic(entry.schematic);
            return true;
        }

        return false;
    }

    public Collection<SchematicEntry> getAllSchematics()
    {
        return this.schematics.values();
    }

    public static class SchematicEntry
    {
        private final LitematicaSchematic schematic;
        private final String schematicName;
        private final int schematicId;
        @Nullable
        private final String fileName;

        public SchematicEntry(LitematicaSchematic schematic, int schematicId, String schematicName, @Nullable String fileName)
        {
            this.schematic = schematic;
            this.schematicId = schematicId;
            this.schematicName = schematicName;
            this.fileName = fileName;
        }

        public LitematicaSchematic getSchematic()
        {
            return this.schematic;
        }

        public int getId()
        {
            return this.schematicId;
        }

        public String getSchematicName()
        {
            return this.schematicName;
        }

        @Nullable
        public String getFileName()
        {
            return this.fileName;
        }
    }
}
