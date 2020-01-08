package fi.dy.masa.litematica.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.SchematicType;

public class SchematicHolder
{
    private static final SchematicHolder INSTANCE = new SchematicHolder();
    private final List<ISchematic> schematics = new ArrayList<>();

    public static SchematicHolder getInstance()
    {
        return INSTANCE;
    }

    public void clearLoadedSchematics()
    {
        this.schematics.clear();
    }

    public List<ISchematic> getAllOf(File file)
    {
        List<ISchematic> list = new ArrayList<>();

        for (ISchematic schematic : this.schematics)
        {
            if (file.equals(schematic.getFile()))
            {
                list.add(schematic);
            }
        }

        return list;
    }

    @Nullable
    public ISchematic getOrLoad(File file)
    {
        if (file.exists() == false || file.isFile() == false || file.canRead() == false)
        {
            return null;
        }

        for (ISchematic schematic : this.schematics)
        {
            if (file.equals(schematic.getFile()))
            {
                return schematic;
            }
        }

        ISchematic schematic = SchematicType.tryCreateSchematicFrom(file);

        if (schematic != null)
        {
            this.schematics.add(schematic);
        }

        return schematic;
    }

    public void addSchematic(ISchematic schematic, boolean allowDuplicates)
    {
        if (allowDuplicates || this.schematics.contains(schematic) == false)
        {
            if (allowDuplicates == false && schematic.getFile() != null)
            {
                for (ISchematic tmp : this.schematics)
                {
                    if (schematic.getFile().equals(tmp.getFile()))
                    {
                        return;
                    }
                }
            }

            this.schematics.add(schematic);
        }
    }

    public boolean removeSchematic(ISchematic schematic)
    {
        if (this.schematics.remove(schematic))
        {
            DataManager.getSchematicPlacementManager().removeAllPlacementsOfSchematic(schematic);
            return true;
        }

        return false;
    }

    public Collection<ISchematic> getAllSchematics()
    {
        return this.schematics;
    }
}
