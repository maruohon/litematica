package fi.dy.masa.litematica.selection;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
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
    private final Map<String, AreaSelection> readOnlySelections = new HashMap<>();
    @Nullable
    private String currentSelectionId;
    @Nullable
    private GrabbedElement grabbedElement;

    @Nullable
    public String getCurrentSelectionId()
    {
        return this.currentSelectionId;
    }

    @Nullable
    public AreaSelection getCurrentSelection()
    {
        return this.getSelection(this.currentSelectionId);
    }

    @Nullable
    public AreaSelection getSelection(String selectionId)
    {
        return selectionId != null ? this.selections.get(selectionId) : null;
    }

    @Nullable
    public AreaSelection getOrLoadSelection(String selectionId)
    {
        AreaSelection selection = this.getSelection(selectionId);

        if (selection == null)
        {
            selection = this.tryLoadSelectionFromFile(selectionId);

            if (selection != null)
            {
                this.selections.put(selectionId, selection);
            }
        }

        return selection;
    }

    @Nullable
    public AreaSelection getOrLoadSelectionReadOnly(String selectionId)
    {
        AreaSelection selection = this.getSelection(selectionId);

        if (selection == null)
        {
            selection = this.readOnlySelections.get(selectionId);

            if (selection == null)
            {
                selection = this.tryLoadSelectionFromFile(selectionId);

                if (selection != null)
                {
                    this.readOnlySelections.put(selectionId, selection);
                }
            }
        }

        return selection;
    }

    @Nullable
    private AreaSelection tryLoadSelectionFromFile(String selectionId)
    {
        File file = new File(selectionId);
        JsonElement el = JsonUtils.parseJsonFile(file);

        if (el != null && el.isJsonObject())
        {
            return AreaSelection.fromJson(el.getAsJsonObject());
        }

        return null;
    }

    public boolean removeSelection(String selectionId)
    {
        if (selectionId != null && this.selections.remove(selectionId) != null)
        {
            File file = new File(selectionId);

            if (file.exists() && file.isFile())
            {
                file.delete();
            }

            return true;
        }

        return false;
    }

    public boolean renameSelection(File dir, String selectionId, String newName, IMessageConsumer feedback)
    {
        File file = new File(selectionId);

        if (file.exists() && file.isFile())
        {
            String newFileName = FileUtils.generateSafeFileName(newName);
            File newFile = new File(dir, newFileName + ".json");

            if (newFile.exists() == false && file.renameTo(newFile))
            {
                AreaSelection selection = this.selections.remove(selectionId);

                if (selection != null)
                {
                    String oldName = selection.getName();
                    String newId = newFile.getAbsolutePath();
                    selection.setName(newName);
                    this.selections.put(newId, selection);

                    if (selectionId.equals(this.currentSelectionId))
                    {
                        this.currentSelectionId = newId;
                    }

                    List<Box> boxes = selection.getAllSubRegionBoxes();

                    // If the selection had only one box with the exact same name as the area selection itself,
                    // then also rename that box to the new name.
                    if (boxes.size() == 1 && boxes.get(0).getName().equals(oldName))
                    {
                        selection.renameSubRegionBox(oldName, newName);
                    }

                    return true;
                }
            }
            else
            {
                feedback.addMessage(MessageType.ERROR, "litematica.error.area_selection.rename.already_exists", newFile.getName());
            }
        }

        return false;
    }

    public boolean renameSelectedSubRegionBox(String newName)
    {
        String selectionId = this.getCurrentSelectionId();

        if (selectionId != null)
        {
            return this.renameSubRegionBox(selectionId, newName);
        }

        return false;
    }

    public boolean renameSubRegionBox(String selectionId, String newName)
    {
        AreaSelection selection = this.getSelection(selectionId);

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

    public boolean renameSubRegionBox(String selectionId, String oldName, String newName)
    {
        AreaSelection selection = this.getSelection(selectionId);
        return selection != null && selection.renameSubRegionBox(oldName, newName);
    }

    public void setCurrentSelection(@Nullable String selectionId)
    {
        if (selectionId == null || this.selections.containsKey(selectionId))
        {
            this.currentSelectionId = selectionId;
        }
    }

    /**
     * Creates a new schematic selection and returns the name of it
     * @return
     */
    public String createNewSelection(File dir, final String nameIn)
    {
        String name = nameIn;
        File file = new File(dir, FileUtils.generateSafeFileName(name) + ".json");
        String selectionId = file.getAbsolutePath();
        int i = 1;

        while (this.selections.containsKey(selectionId) || file.exists())
        {
            name = nameIn + " " + i;
            file = new File(dir, FileUtils.generateSafeFileName(name) + ".json");
            selectionId = file.getAbsolutePath();
            i++;
        }

        AreaSelection selection = new AreaSelection();
        selection.setName(name);

        this.selections.put(selectionId, selection);
        this.currentSelectionId = selectionId;

        JsonUtils.writeJsonToFile(selection.toJson(), file);

        return this.currentSelectionId;
    }

    public boolean createSelectionFromPlacement(File dir, SchematicPlacement placement)
    {
        File file = new File(dir, FileUtils.generateSafeFileName(placement.getName()) + ".json");
        String name = file.getAbsolutePath();
        AreaSelection selection = this.getSelection(name);

        if (selection == null)
        {
            selection = AreaSelection.fromPlacement(placement);

            this.selections.put(name, selection);
            this.currentSelectionId = name;

            JsonUtils.writeJsonToFile(selection.toJson(), file);

            return true;
        }

        return false;
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
        return area != null && (area.getSelectedSubRegionBox() != null || area.isOriginSelected());
    }

    public boolean hasSelectedOrigin()
    {
        AreaSelection area = this.getCurrentSelection();
        return area != null && area.isOriginSelected();
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

    public void setPositionOfCurrentSelectionToRayTrace(Minecraft mc, Corner corner, boolean moveEntireSelection, double maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
        {
            boolean movingCorner = area.getSelectedSubRegionBox() != null && corner != Corner.NONE;
            boolean movingOrigin = area.isOriginSelected();

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
                        area.getSelectedSubRegionBox().setPos1(pos);
                    }
                    else if (corner == Corner.CORNER_2)
                    {
                        area.getSelectedSubRegionBox().setPos2(pos);
                        cornerIndex = 2;
                    }

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSubRegionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    StringUtils.printActionbarMessage("litematica.message.set_selection_box_point", cornerIndex, posStr);
                }
                // Moving the origin point
                else
                {
                    this.moveSelectionOrigin(area, pos, moveEntireSelection);
                }
            }
        }
    }

    public void moveSelectionOrigin(AreaSelection area, BlockPos newOrigin, boolean moveEntireSelection)
    {
        if (moveEntireSelection)
        {
            area.moveEntireSelectionTo(newOrigin, true);
        }
        else
        {
            BlockPos old = area.getOrigin();
            area.setOrigin(newOrigin);

            String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
            String posStrNew = String.format("x: %d, y: %d, z: %d", newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            StringUtils.printActionbarMessage("litematica.message.moved_area_origin", posStrOld, posStrNew);
        }
    }

    public void handleCuboidModeMouseClick(Minecraft mc, double maxDistance, boolean isRightClick, boolean moveEntireSelection)
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection != null)
        {
            if (selection.isOriginSelected())
            {
                BlockPos newOrigin = this.getTargetedPosition(mc.world, mc.player, maxDistance);
                this.moveSelectionOrigin(selection, newOrigin, moveEntireSelection);
            }
            // Right click in Cuboid mode: Reset the area to the clicked position
            else if (isRightClick)
            {
                this.resetSelectionToClickedPosition(mc, maxDistance);
            }
            // Left click in Cuboid mode: Grow the selection to contain each clicked position
            else
            {
                this.growSelectionToContainClickedPosition(mc, maxDistance);
            }
        }
    }

    private void resetSelectionToClickedPosition(Minecraft mc, double maxDistance)
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

    private void growSelectionToContainClickedPosition(Minecraft mc, double maxDistance)
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

    public void clear()
    {
        this.grabbedElement = null;
        this.currentSelectionId = null;
        this.selections.clear();
        this.readOnlySelections.clear();
    }

    public void loadFromJson(JsonObject obj)
    {
        this.clear();

        if (JsonUtils.hasString(obj, "current"))
        {
            String currentId = obj.get("current").getAsString();
            AreaSelection selection = this.tryLoadSelectionFromFile(currentId);

            if (selection != null)
            {
                this.selections.put(currentId, selection);
                this.setCurrentSelection(currentId);
            }
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        try
        {
            for (Map.Entry<String, AreaSelection> entry : this.selections.entrySet())
            {
                JsonUtils.writeJsonToFile(entry.getValue().toJson(), new File(entry.getKey()));
            }
        }
        catch (Exception e)
        {
            LiteModLitematica.logger.warn("Exception while writing area selections to disk", e);
        }

        AreaSelection current = this.currentSelectionId != null ? this.selections.get(this.currentSelectionId) : null;

        // Clear the loaded selections, except for the currently selected one
        this.selections.clear();
        this.readOnlySelections.clear();

        if (current != null)
        {
            obj.add("current", new JsonPrimitive(this.currentSelectionId));
            this.selections.put(this.currentSelectionId, current);
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
