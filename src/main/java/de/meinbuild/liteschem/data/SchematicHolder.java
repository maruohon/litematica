package de.meinbuild.liteschem.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

import de.meinbuild.liteschem.schematic.LitematicaSchematic;

public class SchematicHolder
{
    private static final SchematicHolder INSTANCE = new SchematicHolder();
    private final List<LitematicaSchematic> schematics = new ArrayList<>();

    public static SchematicHolder getInstance()
    {
        return INSTANCE;
    }

    public void clearLoadedSchematics()
    {
        this.schematics.clear();
    }

    @Nullable
    public LitematicaSchematic getOrLoad(File file)
    {
        for (LitematicaSchematic schematic : this.schematics)
        {
            if (file.equals(schematic.getFile()))
            {
                return schematic;
            }
        }

        LitematicaSchematic schematic = LitematicaSchematic.createFromFile(file.getParentFile(), file.getName());

        if (schematic != null)
        {
            this.schematics.add(schematic);
        }

        return schematic;
    }

    public void addSchematic(LitematicaSchematic schematic, boolean allowDuplicates)
    {
        if (allowDuplicates || this.schematics.contains(schematic) == false)
        {
            if (allowDuplicates == false && schematic.getFile() != null)
            {
                for (LitematicaSchematic tmp : this.schematics)
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

    public boolean removeSchematic(LitematicaSchematic schematic)
    {
        if (this.schematics.remove(schematic))
        {
            DataManager.getSchematicPlacementManager().removeAllPlacementsOfSchematic(schematic);
            return true;
        }

        return false;
    }

    public Collection<LitematicaSchematic> getAllSchematics()
    {
        return this.schematics;
    }
}
