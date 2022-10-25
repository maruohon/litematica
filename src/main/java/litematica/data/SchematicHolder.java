package litematica.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import litematica.schematic.ISchematic;
import litematica.schematic.SchematicType;

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

    public List<ISchematic> getAllOf(Path file)
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
    public ISchematic getOrLoad(Path file)
    {
        if (Files.isRegularFile(file) == false || Files.isReadable(file) == false)
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

    public List<ISchematic> getAllSchematics()
    {
        return this.schematics;
    }
}
