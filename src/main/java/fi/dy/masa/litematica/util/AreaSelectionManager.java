package fi.dy.masa.litematica.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.schematic.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

public class AreaSelectionManager
{
    private final Map<String, AreaSelection> selections = new HashMap<>();
    private String currentSelection = "Unnamed 1";

    public Collection<String> getAllAreaSelectionNames()
    {
        return this.selections.keySet();
    }

    public Collection<AreaSelection> getAllAreaSelections()
    {
        return this.selections.values();
    }

    public String getCurrentAreaSelectionName()
    {
        return this.currentSelection;
    }

    public void setCurrentAreaSelection(String name)
    {
        if (this.selections.containsKey(name))
        {
            this.currentSelection = name;
        }
    }

    /**
     * Creates a new schematic selection and returns the name of it
     * @return
     */
    public String createNewAreaSelection()
    {
        String name = "Unnamed ";
        int i = 1;

        while (this.selections.containsKey(name + i))
        {
            i++;
        }

        this.selections.put(name + i, new AreaSelection());
        this.currentSelection = name + i;

        return this.currentSelection;
    }

    @Nullable
    public AreaSelection getAreaSelection(String name)
    {
        return this.selections.get(name);
    }

    @Nullable
    public AreaSelection getSelectedAreaSelection()
    {
        return this.getAreaSelection(this.currentSelection);
    }

    public boolean removeAreaSelection(String name)
    {
        return this.selections.remove(name) != null;
    }

    public boolean removeSelectedAreaSelection()
    {
        return this.selections.remove(this.currentSelection) != null;
    }

    public boolean renameAreaSelection(String oldName, String newName)
    {
        AreaSelection selection = this.selections.remove(oldName);

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

    public boolean changeSelection(World world, Entity entity)
    {
        AreaSelection area = this.getSelectedAreaSelection();

        if (area != null && area.getAllSelectionsBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, 200);

            if (trace.getHitType() == HitType.CORNER || trace.getHitType() == HitType.BOX)
            {
                SelectionBox box = area.getSelectedSelectionBox();

                // Clear the selected corner from any current boxes
                if (box != null)
                {
                    box.setSelectedCorner(Corner.NONE);
                }

                box = trace.getHitSelectionBox();
                area.setSelectedBox(box.getName());
                box.setSelectedCorner(trace.getHitCorner());

                return true;
            }
        }

        return false;
    }

    public void loadFromJson(JsonObject obj)
    {
        this.selections.clear();

        if (JsonUtils.hasArray(obj, "areas"))
        {
            JsonArray arr = obj.get("areas").getAsJsonArray();
            final int size = arr.size();

            for (int i = 0; i < size; i++)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    AreaSelection area = AreaSelection.fromJson(el.getAsJsonObject());
                    this.selections.put(area.getName(), area);
                }
            }
        }

        if (JsonUtils.hasString(obj, "current"))
        {
            this.setCurrentAreaSelection(obj.get("current").getAsString());
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (AreaSelection area : this.selections.values())
        {
            arr.add(area.toJson());
        }

        if (arr.size() > 0)
        {
            obj.add("current", new JsonPrimitive(this.currentSelection));
            obj.add("areas", arr);
        }

        return obj;
    }
}
