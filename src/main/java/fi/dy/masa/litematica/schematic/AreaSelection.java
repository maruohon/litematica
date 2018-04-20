package fi.dy.masa.litematica.schematic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.util.JsonUtils;
import net.minecraft.util.math.BlockPos;

public class AreaSelection
{
    private final Map<String, SelectionBox> selectionBoxes = new HashMap<>();
    private String name = "Unnamed";
    private String currentBox = "Box 1";

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCurrentSelectionBoxName()
    {
        return this.currentBox;
    }

    @Nullable
    public SelectionBox getSelectionBox(String name)
    {
        return this.selectionBoxes.get(name);
    }

    @Nullable
    public SelectionBox getSelectedSelectionBox()
    {
        return this.selectionBoxes.get(this.currentBox);
    }

    public Collection<SelectionBox> getAllSelectionsBoxes()
    {
        return this.selectionBoxes.values();
    }

    public String createNewSelectionBox(BlockPos pos1)
    {
        String name = "Box ";
        int i = 1;

        while (this.selectionBoxes.containsKey(name + i))
        {
            i++;
        }

        SelectionBox box = new SelectionBox();
        box.setName(name + i);
        box.setPos1(pos1);

        this.selectionBoxes.put(name + i, box);
        this.currentBox = name + i;

        return this.currentBox;
    }

    /**
     * Adds the given SelectionBox, if either replace is true, or there isn't yet a box by the same name.
     * @param box
     * @param replace
     * @return true if the box was successfully added, false if replace was false and there was already a box with the same name
     */
    public boolean addSelectionBox(SelectionBox box, boolean replace)
    {
        if (replace || this.selectionBoxes.containsKey(box.getName()) == false)
        {
            this.selectionBoxes.put(box.getName(), box);
            return true;
        }

        return false;
    }

    public void removeAllSelectionBoxes()
    {
        this.selectionBoxes.clear();
    }

    public boolean removeSelectionBox(String name)
    {
        return this.selectionBoxes.remove(name) != null;
    }

    public boolean removeSelectedSelectionBox()
    {
        return this.selectionBoxes.remove(this.currentBox) != null;
    }

    public static AreaSelection fromJson(JsonObject obj)
    {
        AreaSelection area = new AreaSelection();

        if (JsonUtils.hasArray(obj, "boxes"))
        {
            JsonArray arr = obj.get("boxes").getAsJsonArray();
            final int size = arr.size();

            for (int i = 0; i < size; i++)
            {
                JsonElement el = arr.get(i);

                if (el.isJsonObject())
                {
                    SelectionBox box = SelectionBox.fromJson(el.getAsJsonObject());

                    if (box != null)
                    {
                        area.selectionBoxes.put(box.getName(), box);
                    }
                }
            }
        }

        if (JsonUtils.hasString(obj, "name"))
        {
            area.name = obj.get("name").getAsString();
        }

        if (JsonUtils.hasString(obj, "current"))
        {
            area.currentBox = obj.get("current").getAsString();
        }

        return area;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (SelectionBox box : this.selectionBoxes.values())
        {
            JsonObject o = box.toJson();

            if (o != null)
            {
                arr.add(o);
            }
        }

        obj.add("name", new JsonPrimitive(this.name));

        if (arr.size() > 0)
        {
            obj.add("current", new JsonPrimitive(this.currentBox));
            obj.add("boxes", arr);
        }

        return obj;
    }
}
