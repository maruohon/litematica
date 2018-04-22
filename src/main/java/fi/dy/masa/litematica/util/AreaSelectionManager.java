package fi.dy.masa.litematica.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.config.KeyCallbacks;
import fi.dy.masa.litematica.schematic.AreaSelection;
import fi.dy.masa.litematica.schematic.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AreaSelectionManager
{
    private final Map<String, AreaSelection> selections = new HashMap<>();
    @Nullable
    private String currentSelection;
    private GrabbedElement grabbedElement;

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
        return this.currentSelection != null ? this.currentSelection : "";
    }

    public void setCurrentAreaSelection(@Nullable String name)
    {
        if (name == null || this.selections.containsKey(name))
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
        return this.currentSelection != null ? this.getAreaSelection(this.currentSelection) : null;
    }

    public boolean removeAreaSelection(String name)
    {
        return this.selections.remove(name) != null;
    }

    public boolean removeSelectedAreaSelection()
    {
        return this.currentSelection != null ? this.selections.remove(this.currentSelection) != null : false;
    }

    public boolean renameAreaSelection(String oldName, String newName)
    {
        AreaSelection selection = this.selections.remove(oldName);

        if (selection != null)
        {
            selection.setName(newName);
            this.selections.put(newName, selection);

            if (this.currentSelection != null && this.currentSelection.equals(oldName))
            {
                this.currentSelection = newName;
            }

            return true;
        }

        return false;
    }

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        AreaSelection area = this.getSelectedAreaSelection();

        if (area != null && area.getAllSelectionsBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            if (trace.getHitType() == HitType.CORNER || trace.getHitType() == HitType.BOX)
            {
                this.changeSelection(area, trace);
                return true;
            }
            else if (trace.getHitType() == HitType.ORIGIN)
            {
                return true;
            }
            else if (trace.getHitType() == HitType.MISS)
            {
                SelectionBox box = area.getSelectedSelectionBox();

                if (box != null)
                {
                    box.setSelectedCorner(Corner.NONE);
                }

                area.setSelectedBox(null);
                return true;
            }
        }

        return false;
    }

    private void changeSelection(AreaSelection area, RayTraceWrapper trace)
    {
        SelectionBox box = area.getSelectedSelectionBox();

        // Clear the selected corner from any current boxes
        if (box != null)
        {
            box.setSelectedCorner(Corner.NONE);
        }

        if (trace.getHitType() == HitType.CORNER || trace.getHitType() == HitType.BOX)
        {
            box = trace.getHitSelectionBox();
            area.setSelectedBox(box.getName());
            box.setSelectedCorner(trace.getHitCorner());
        }
        else if (trace.getHitType() == HitType.ORIGIN)
        {
            
        }
    }

    public boolean hasSelectedElement()
    {
        AreaSelection area = this.getSelectedAreaSelection();
        return area != null && area.getSelectedSelectionBox() != null;
    }

    public void moveSelectedElement(EnumFacing direction, int amount)
    {
        AreaSelection area = this.getSelectedAreaSelection();

        if (area != null && area.getSelectedSelectionBox() != null)
        {
            SelectionBox box = area.getSelectedSelectionBox();
            Corner selectedCorner = box.getSelectedCorner();

            if ((selectedCorner == Corner.NONE || selectedCorner == Corner.CORNER_1) && box.getPos1() != null)
            {
                box.setPos1(box.getPos1().offset(direction, amount));
            }

            if ((selectedCorner == Corner.NONE || selectedCorner == Corner.CORNER_2) && box.getPos2() != null)
            {
                box.setPos2(box.getPos2().offset(direction, amount));
            }
        }
    }

    public boolean hasGrabbedElement()
    {
        return this.grabbedElement != null;
    }

    public boolean grabElement(Minecraft mc, int maxDistance)
    {
        World world = mc.world;
        Entity entity = mc.player;
        AreaSelection area = this.getSelectedAreaSelection();

        if (area != null && area.getAllSelectionsBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            if (trace.getHitType() == HitType.CORNER || trace.getHitType() == HitType.BOX)
            {
                this.changeSelection(area, trace);
                this.grabbedElement = new GrabbedElement(
                        trace.getHitSelectionBox(),
                        trace.getHitCorner(),
                        trace.getHitVec(),
                        entity.getPositionEyes(1f).distanceTo(trace.getHitVec()));
                KeyCallbacks.printMessage(mc, "litematica.message.grabbed_element_for_moving");
                return true;
            }
        }

        return false;
    }

    public void releaseGrabbedElement()
    {
        this.grabbedElement = null;
    }

    public void changeGrabDistance(Entity entity, double amount)
    {
        if (this.grabbedElement != null)
        {
            this.grabbedElement.changeGrabDistance(amount);
            this.grabbedElement.moveElement(entity);
        }
    }

    public void moveGrabbedElement(Entity entity)
    {
        if (this.grabbedElement != null)
        {
            this.grabbedElement.moveElement(entity);
        }
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
            if (this.currentSelection != null)
            {
                obj.add("current", new JsonPrimitive(this.currentSelection));
            }

            obj.add("areas", arr);
        }

        return obj;
    }

    private static class GrabbedElement
    {
        public final SelectionBox grabbedBox;
        public final SelectionBox originalBox;
        public final Vec3d grabPosition;
        public final Corner grabbedCorner;
        public double grabDistance;

        private GrabbedElement(SelectionBox box, Corner corner, Vec3d grabPosition, double grabDistance)
        {
            this.grabbedBox = box;
            this.grabbedCorner = corner;
            this.grabPosition = grabPosition;
            this.grabDistance = grabDistance;
            this.originalBox = new SelectionBox();
            this.originalBox.setPos1(box.getPos1());
            this.originalBox.setPos2(box.getPos2());
        }

        public void changeGrabDistance(double amount)
        {
            this.grabDistance += amount;
        }

        public void moveElement(Entity entity)
        {
            Vec3d newLookPos = entity.getPositionEyes(1f).add(entity.getLook(1f).scale(this.grabDistance));
            Vec3d change = newLookPos.subtract(this.grabPosition);

            if ((this.grabbedCorner == Corner.NONE || this.grabbedCorner == Corner.CORNER_1) && this.grabbedBox.getPos1() != null)
            {
                this.grabbedBox.setPos1(this.originalBox.getPos1().add(change.x, change.y, change.z));
            }

            if ((this.grabbedCorner == Corner.NONE || this.grabbedCorner == Corner.CORNER_2) && this.grabbedBox.getPos2() != null)
            {
                this.grabbedBox.setPos2(this.originalBox.getPos2().add(change.x, change.y, change.z));
            }
        }
    }
}
