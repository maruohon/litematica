package fi.dy.masa.litematica.selection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.util.JsonUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SelectionManager
{
    private final Map<String, AreaSelection> selections = new HashMap<>();
    @Nullable
    private String currentSelection;
    private GrabbedElement grabbedElement;

    public Collection<String> getAllSelectionNames()
    {
        return this.selections.keySet();
    }

    public Collection<AreaSelection> getAllSelections()
    {
        return this.selections.values();
    }

    public String getCurrentSelectionName()
    {
        return this.currentSelection != null ? this.currentSelection : "";
    }

    @Nullable
    public AreaSelection getCurrentSelection()
    {
        return this.currentSelection != null ? this.getSelection(this.currentSelection) : null;
    }

    @Nullable
    public AreaSelection getSelection(String name)
    {
        return this.selections.get(name);
    }

    public boolean removeSelection(String name)
    {
        return this.selections.remove(name) != null;
    }

    public boolean removeCurrentSelection()
    {
        return this.currentSelection != null ? this.selections.remove(this.currentSelection) != null : false;
    }

    public boolean renameSelection(String oldName, String newName)
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

    public boolean renameSelectedSubRegionBox(String newName)
    {
        String selectedArea = this.getCurrentSelectionName();

        if (selectedArea != null)
        {
            return this.renameSelectedSubRegionBox(selectedArea, newName);
        }

        return false;
    }

    public boolean renameSelectedSubRegionBox(String selectionName, String newName)
    {
        AreaSelection selection = this.selections.get(selectionName);

        if (selection != null)
        {
            String oldName = selection.getCurrentSubRegionBoxName();

            if (oldName != null)
            {
                return selection.renameSubRegionBox(oldName, newName);
            }
        }

        return false;
    }

    public boolean renameSubRegionBox(String selectionName, String oldName, String newName)
    {
        AreaSelection selection = this.selections.get(selectionName);
        return selection != null && selection.renameSubRegionBox(oldName, newName);
    }

    public void setCurrentSelection(@Nullable String name)
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
    public String createNewSelection()
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

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            if (trace.getHitType() == HitType.SELECTION_BOX_CORNER || trace.getHitType() == HitType.SELECTION_BOX_BODY || trace.getHitType() == HitType.SELECTION_ORIGIN)
            {
                this.changeSelection(area, trace);
                return true;
            }
            else if (trace.getHitType() == HitType.MISS)
            {
                area.clearCurrentSelectedCorner();
                area.setSelectedSubRegionBox(null);
                area.setOriginSelected(false);
                return true;
            }
        }

        return false;
    }

    private void changeSelection(AreaSelection area, RayTraceWrapper trace)
    {
        area.clearCurrentSelectedCorner();

        if (trace.getHitType() == HitType.SELECTION_BOX_CORNER || trace.getHitType() == HitType.SELECTION_BOX_BODY)
        {
            Box box = trace.getHitSelectionBox();
            area.setSelectedSubRegionBox(box.getName());
            area.setOriginSelected(false);
            box.setSelectedCorner(trace.getHitCorner());
        }
        else if (trace.getHitType() == HitType.SELECTION_ORIGIN)
        {
            area.setSelectedSubRegionBox(null);
            area.setOriginSelected(true);
        }
    }

    public boolean hasSelectedElement()
    {
        AreaSelection area = this.getCurrentSelection();
        return area != null && area.getSelectedSubRegionBox() != null;
    }

    public void moveSelectedElement(EnumFacing direction, int amount)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
        {
            if (area.getSelectedSubRegionBox() != null)
            {
                Box box = area.getSelectedSubRegionBox();
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
            else if (area.isOriginSelected())
            {
                area.setOrigin(area.getOrigin().offset(direction, amount));
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
        AreaSelection area = this.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            if (trace.getHitType() == HitType.SELECTION_BOX_CORNER || trace.getHitType() == HitType.SELECTION_BOX_BODY)
            {
                this.changeSelection(area, trace);
                this.grabbedElement = new GrabbedElement(
                        trace.getHitSelectionBox(),
                        trace.getHitCorner(),
                        trace.getHitVec(),
                        entity.getPositionEyes(1f).distanceTo(trace.getHitVec()));
                StringUtils.printActionbarMessage("litematica.message.grabbed_element_for_moving");
                return true;
            }
        }

        return false;
    }

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, Corner corner, double maxDistance)
    {
        AreaSelection sel = this.getCurrentSelection();

        if (sel != null)
        {
            boolean movingCorner = sel.getSelectedSubRegionBox() != null && corner != Corner.NONE;
            boolean movingOrigin = sel.isOriginSelected();

            if (movingCorner || movingOrigin)
            {
                BlockPos pos = this.getTargetedPosition(mc.world, mc.player, maxDistance);

                if (pos == null)
                {
                    return;
                }

                if (movingCorner)
                {
                    int cornerIndex = 1;

                    if (corner == Corner.CORNER_1)
                    {
                        sel.getSelectedSubRegionBox().setPos1(pos);
                    }
                    else if (corner == Corner.CORNER_2)
                    {
                        sel.getSelectedSubRegionBox().setPos2(pos);
                        cornerIndex = 2;
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    StringUtils.printActionbarMessage("litematica.message.set_selection_box_point", cornerIndex, posStr);
                }
                // Moving the origin point
                else
                {
                    BlockPos old = sel.getOrigin();
                    sel.setOrigin(pos);
                    String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
                    String posStrNew = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    StringUtils.printActionbarMessage("litematica.message.moved_area_origin", posStrOld, posStrNew);
                }
            }
        }
    }

    public void resetSelectionToClickedPosition(Minecraft mc, double maxDistance)
    {
        AreaSelection sel = this.getCurrentSelection();

        if (sel != null && sel.getSelectedSubRegionBox() != null)
        {
            BlockPos pos = this.getTargetedPosition(mc.world, mc.player, maxDistance);

            if (pos != null)
            {
                sel.getSelectedSubRegionBox().setPos1(pos);
                sel.getSelectedSubRegionBox().setPos2(pos);
            }
        }
    }

    public void growSelectionToContainClickedPosition(Minecraft mc, double maxDistance)
    {
        AreaSelection sel = this.getCurrentSelection();

        if (sel != null && sel.getSelectedSubRegionBox() != null)
        {
            BlockPos pos = this.getTargetedPosition(mc.world, mc.player, maxDistance, false);

            if (pos != null)
            {
                Box box = sel.getSelectedSubRegionBox();
                BlockPos pos1 = box.getPos1();
                BlockPos pos2 = box.getPos2();

                if (pos1 == null)
                {
                    pos1 = pos;
                }

                if (pos2 == null)
                {
                    pos2 = pos;
                }

                BlockPos posMin = PositionUtils.getMinCorner(PositionUtils.getMinCorner(pos1, pos2), pos);
                BlockPos posMax = PositionUtils.getMaxCorner(PositionUtils.getMaxCorner(pos1, pos2), pos);

                box.setPos1(posMin);
                box.setPos2(posMax);
            }
        }
    }

    @Nullable
    private BlockPos getTargetedPosition(World world, EntityPlayer player, double maxDistance)
    {
        return getTargetedPosition(world, player, maxDistance, true);
    }

    @Nullable
    private BlockPos getTargetedPosition(World world, EntityPlayer player, double maxDistance, boolean sneakToInset)
    {
        RayTraceResult trace = RayTraceUtils.getRayTraceFromEntity(world, player, false, maxDistance);

        if (trace.typeOfHit != RayTraceResult.Type.BLOCK)
        {
            return null;
        }

        BlockPos pos = trace.getBlockPos();

        // Sneaking puts the position inside the targeted block, not sneaking puts it against the targeted face
        if (player.isSneaking() != sneakToInset)
        {
            pos = pos.offset(trace.sideHit);
        }

        return pos;
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
            this.setCurrentSelection(obj.get("current").getAsString());
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
        public final Box grabbedBox;
        public final Box originalBox;
        public final Vec3d grabPosition;
        public final Corner grabbedCorner;
        public double grabDistance;

        private GrabbedElement(Box box, Corner corner, Vec3d grabPosition, double grabDistance)
        {
            this.grabbedBox = box;
            this.grabbedCorner = corner;
            this.grabPosition = grabPosition;
            this.grabDistance = grabDistance;
            this.originalBox = new Box();
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
