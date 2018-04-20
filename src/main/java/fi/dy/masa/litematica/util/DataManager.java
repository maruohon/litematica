package fi.dy.masa.litematica.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.schematic.SchematicPlacement;
import fi.dy.masa.litematica.schematic.SchematicSelection;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class DataManager
{
    private static final Int2ObjectOpenHashMap<DataManager> INSTANCES = new Int2ObjectOpenHashMap<>();

    private final List<SchematicPlacement> loadedSchematics = new ArrayList<>();
    private final Map<String, SchematicSelection> selections = new HashMap<>();
    private final Minecraft mc;
    private File lastSchematicDirectory; // TODO use a custom class with split directories?
    private String currentWorld = "default";
    private String currentSelection = "Unnamed 1";

    private DataManager()
    {
        this.mc = Minecraft.getMinecraft();
        this.lastSchematicDirectory = new File(this.mc.mcDataDir, "schematics");
    }

    public static DataManager getInstance(World world)
    {
        final int dimension = world.provider.getDimensionType().getId();
        DataManager instance = INSTANCES.get(dimension);

        if (instance == null)
        {
            instance = new DataManager();
            INSTANCES.put(dimension, instance);
        }

        return instance;
    }

    public File getSchematicDirectory()
    {
        return this.lastSchematicDirectory;
    }

    public void setSchematicDirectory(File dir)
    {
        this.lastSchematicDirectory = dir;
    }

    public List<SchematicPlacement> getLoadedSchematicsForDimension(World world)
    {
        List<SchematicPlacement> list = new ArrayList<>();

        if (world != null)
        {
            final int dimension = world.provider.getDimensionType().getId();

            for (SchematicPlacement placement : this.loadedSchematics)
            {
                if (placement.getDimension() == dimension)
                {
                    list.add(placement);
                }
            }
        }

        return list;
    }

    public void loadSchematic(World world, SchematicPlacement placement)
    {
        if (this.loadedSchematics.contains(placement) == false)
        {
            this.loadedSchematics.add(placement);
        }
        else
        {
            this.mc.ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentTranslation("litematica.error.duplicate_schematic_load"));
        }
    }

    /**
     * Creates a new schematic selection and returns the name of it
     * @return
     */
    public String createNewSelection()
    {
        String name = "Unnamed ";
        int i = 1;

        while (this.selections.containsKey(name + i))
        {
            i++;
        }

        this.selections.put(name + i, new SchematicSelection());
        this.currentSelection = name + i;

        return this.currentSelection;
    }

    @Nullable
    public SchematicSelection getSelection(String name)
    {
        return this.selections.get(name);
    }

    public boolean removeSelection(String name)
    {
        return this.selections.remove(name) != null;
    }

    public boolean renameSelection(String oldName, String newName)
    {
        SchematicSelection selection = this.selections.remove(oldName);

        if (selection != null)
        {
            selection.setName(newName);
            this.selections.put(newName, selection);

            if (this.currentSelection.equals(oldName))
            {
                this.currentSelection = newName;
            }

            return true;
        }

        return false;
    }

    public Collection<String> getAllSelectionNames()
    {
        return this.selections.keySet();
    }

    public Collection<SchematicSelection> getAllSelections()
    {
        return this.selections.values();
    }

    public String getCurrentSelectionName()
    {
        return this.currentSelection;
    }

    public void setCurrentSelection(String name)
    {
        if (this.selections.containsKey(name))
        {
            this.currentSelection = name;
        }
    }

    public void load()
    {
        
    }

    public void save()
    {
        
    }
}
