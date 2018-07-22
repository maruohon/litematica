package fi.dy.masa.litematica.selection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.data.Placement.RequiredEnabled;
import fi.dy.masa.litematica.data.SchematicPlacement;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import net.minecraft.util.math.BlockPos;

public class AreaSelection
{
    private final Map<String, Box> subRegionBoxes = new HashMap<>();
    private BlockPos origin = BlockPos.ORIGIN;
    private String name = "Unnamed";
    @Nullable
    private String currentBox;
    private boolean originSelected;

    public static AreaSelection fromSchematic(LitematicaSchematic schematic, SchematicPlacement placement)
    {
        Map<String, Box> boxes = schematic.getAreas();
        BlockPos origin = placement.getOrigin();
        AreaSelection selection = new AreaSelection();
        selection.name = placement.getName();

        for (Map.Entry<String, Box> entry : boxes.entrySet())
        {
            Box box = entry.getValue();
            BlockPos pos1 = PositionUtils.getTransformedBlockPos(box.getPos1(), placement.getMirror(), placement.getRotation()).add(origin);
            BlockPos pos2 = PositionUtils.getTransformedBlockPos(box.getPos2(), placement.getMirror(), placement.getRotation()).add(origin);
            box = new Box(pos1, pos2, entry.getKey());
            selection.subRegionBoxes.put(box.getName(), box);
        }

        return selection;
    }

    public static AreaSelection fromPlacement(SchematicPlacement placement)
    {
        ImmutableMap<String, Box> boxes = placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED);
        BlockPos origin = placement.getOrigin();

        AreaSelection selection = new AreaSelection();
        selection.origin = origin;
        selection.name = placement.getName();
        selection.subRegionBoxes.putAll(boxes);

        return selection;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Nullable
    public String getCurrentSubRegionBoxName()
    {
        return this.currentBox;
    }

    public boolean setSelectedSubRegionBox(@Nullable String name)
    {
        if (name == null || this.subRegionBoxes.containsKey(name))
        {
            this.currentBox = name;
            return true;
        }

        return false;
    }

    public boolean isOriginSelected()
    {
        return this.originSelected;
    }

    public void setOriginSelected(boolean selected)
    {
        this.originSelected = selected;
    }

    public BlockPos getOrigin()
    {
        return this.origin;
    }

    public void setOrigin(BlockPos origin)
    {
        this.origin = origin;
    }

    @Nullable
    public Box getSubRegionBox(String name)
    {
        return this.subRegionBoxes.get(name);
    }

    @Nullable
    public Box getSelectedSubRegionBox()
    {
        return this.currentBox != null ? this.subRegionBoxes.get(this.currentBox) : null;
    }

    public Collection<Box> getAllSubRegionBoxes()
    {
        return this.subRegionBoxes.values();
    }

    public String createNewSubRegionBox(BlockPos pos1)
    {
        this.clearCurrentSelectedCorner();

        if (this.origin.equals(BlockPos.ORIGIN) && this.subRegionBoxes.isEmpty())
        {
            this.origin = pos1;
        }

        String name = "Box ";
        int i = 1;

        while (this.subRegionBoxes.containsKey(name + i))
        {
            i++;
        }

        Box box = new Box();
        box.setName(name + i);
        box.setPos1(pos1);
        box.setSelectedCorner(Corner.CORNER_1);

        this.subRegionBoxes.put(name + i, box);
        this.currentBox = name + i;

        return this.currentBox;
    }

    public void clearCurrentSelectedCorner()
    {
        Box box = this.getSelectedSubRegionBox();

        if (box != null)
        {
            box.setSelectedCorner(Corner.NONE);
        }
    }

    /**
     * Adds the given SelectionBox, if either replace is true, or there isn't yet a box by the same name.
     * @param box
     * @param replace
     * @return true if the box was successfully added, false if replace was false and there was already a box with the same name
     */
    public boolean addSubRegionBox(Box box, boolean replace)
    {
        if (replace || this.subRegionBoxes.containsKey(box.getName()) == false)
        {
            this.subRegionBoxes.put(box.getName(), box);
            return true;
        }

        return false;
    }

    public void removeAllSubRegionBoxes()
    {
        this.subRegionBoxes.clear();
    }

    public boolean removeSubRegionBox(String name)
    {
        return this.subRegionBoxes.remove(name) != null;
    }

    public boolean removeSelectedSubRegionBox()
    {
        boolean success = this.currentBox != null ? this.subRegionBoxes.remove(this.currentBox) != null : false;
        this.currentBox = null;
        return success;
    }

    public boolean renameSubRegionBox(String oldName, String newName)
    {
        Box box = this.subRegionBoxes.remove(oldName);

        if (box != null)
        {
            box.setName(newName);
            this.subRegionBoxes.put(newName, box);

            if (this.currentBox != null && this.currentBox.equals(oldName))
            {
                this.currentBox = newName;
            }

            return true;
        }

        return false;
    }

    public void moveEntireSelectionTo(BlockPos newOrigin)
    {
        BlockPos diff = newOrigin.subtract(this.origin);

        for (Box box : this.subRegionBoxes.values())
        {
            if (box.getPos1() != null)
            {
                box.setPos1(box.getPos1().add(diff));
            }

            if (box.getPos2() != null)
            {
                box.setPos2(box.getPos2().add(diff));
            }
        }

        this.origin = newOrigin;
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
                    Box box = Box.fromJson(el.getAsJsonObject());

                    if (box != null)
                    {
                        area.subRegionBoxes.put(box.getName(), box);
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

        BlockPos pos = JsonUtils.blockPosFromJson(obj, "origin");

        if (pos != null)
        {
            area.origin = pos;
        }

        return area;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();
        JsonArray arr = new JsonArray();

        for (Box box : this.subRegionBoxes.values())
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
            if (this.currentBox != null)
            {
                obj.add("current", new JsonPrimitive(this.currentBox));
            }

            obj.add("boxes", arr);
        }

        obj.add("origin", JsonUtils.blockPosToJson(this.origin));

        return obj;
    }
}
