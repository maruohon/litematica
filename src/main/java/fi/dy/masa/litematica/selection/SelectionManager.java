package fi.dy.masa.litematica.selection;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorNormal;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorSimple;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.interfaces.IMessageConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;

public class SelectionManager
{
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, AreaSelection> selections = new HashMap<>();
    private final Map<String, AreaSelection> readOnlySelections = new HashMap<>();
    @Nullable
    private String currentSelectionId;
    @Nullable
    private GrabbedElement grabbedElement;
    private SelectionMode mode = SelectionMode.SIMPLE;

    public SelectionMode getSelectionMode()
    {
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
            return project != null ? project.getSelectionMode() : SelectionMode.SIMPLE;
        }

        return this.mode;
    }

    public void switchSelectionMode()
    {
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

            if (project != null)
            {
                project.switchSelectionMode();
            }
            else
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.error.schematic_projects.in_projects_mode_but_no_project_open");
            }
        }
        else
        {
            this.mode = this.mode.cycle(true);
        }
    }

    @Nullable
    public String getCurrentSelectionId()
    {
        return this.mode == SelectionMode.NORMAL ? this.currentSelectionId : null;
    }

    @Nullable
    public String getCurrentNormalSelectionId()
    {
        return this.currentSelectionId;
    }

    public boolean hasNormalSelection()
    {
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            return true;
        }

        return this.getNormalSelection(this.currentSelectionId) != null;
    }

    @Nullable
    public AreaSelection getCurrentSelection()
    {
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

            if (project != null)
            {
                return project.getSelection();
            }
        }

        return this.getSelection(this.currentSelectionId);
    }

    @Nullable
    public AreaSelection getSelection(@Nullable String selectionId)
    {
        if (this.mode == SelectionMode.SIMPLE)
        {
            return this.getSimpleSelection();
        }

        return this.getNormalSelection(selectionId);
    }

    protected AreaSelectionSimple getSimpleSelection()
    {
        return DataManager.getSimpleArea();
    }

    @Nullable
    protected AreaSelection getNormalSelection(@Nullable String selectionId)
    {
        return selectionId != null ? this.selections.get(selectionId) : null;
    }

    @Nullable
    public AreaSelection getOrLoadSelection(String selectionId)
    {
        AreaSelection selection = this.getNormalSelection(selectionId);

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
        AreaSelection selection = this.getNormalSelection(selectionId);

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
        return tryLoadSelectionFromFile(new File(selectionId));
    }

    @Nullable
    public static AreaSelection tryLoadSelectionFromFile(File file)
    {
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

    public boolean renameSelection(String selectionId, String newName, IMessageConsumer feedback)
    {
        File dir = new File(selectionId);
        dir = dir.getParentFile();

        return this.renameSelection(dir, selectionId, newName, feedback);
    }

    public boolean renameSelection(File dir, String selectionId, String newName, IMessageConsumer feedback)
    {
        return this.renameSelection(dir, selectionId, newName, false, feedback);
    }

    public boolean renameSelection(File dir, String selectionId, String newName, boolean copy, IMessageConsumer feedback)
    {
        File file = new File(selectionId);

        if (file.exists() && file.isFile())
        {
            String newFileName = FileUtils.generateSafeFileName(newName);

            if (newFileName.isEmpty())
            {
                feedback.addMessage(MessageType.ERROR, "litematica.error.area_selection.rename.invalid_safe_file_name", newFileName);
                return false;
            }

            File newFile = new File(dir, newFileName + ".json");

            if (newFile.exists() == false && (copy || file.renameTo(newFile)))
            {
                String newId = newFile.getAbsolutePath();
                AreaSelection selection;

                if (copy)
                {
                    try
                    {
                        org.apache.commons.io.FileUtils.copyFile(file, newFile);
                    }
                    catch (Exception e)
                    {
                        feedback.addMessage(MessageType.ERROR, "litematica.error.area_selection.copy_failed");
                        Litematica.logger.warn("Copy failed", e);
                        return false;
                    }

                    selection = this.getOrLoadSelection(newId);
                }
                else
                {
                    selection = this.selections.remove(selectionId);
                }

                if (selection != null)
                {
                    renameSubRegionBoxIfSingle(selection, newName);
                    selection.setName(newName);

                    this.selections.put(newId, selection);

                    if (selectionId.equals(this.currentSelectionId))
                    {
                        this.currentSelectionId = newId;
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

    public void setCurrentSelection(@Nullable String selectionId)
    {
        this.currentSelectionId = selectionId;

        if (this.currentSelectionId != null)
        {
            this.getOrLoadSelection(this.currentSelectionId);
        }
    }

    /**
     * Creates a new schematic selection and returns the name of it
     * @return
     */
    public String createNewSelection(File dir, final String nameIn)
    {
        String name = nameIn;
        String safeName = FileUtils.generateSafeFileName(name);
        File file = new File(dir, safeName + ".json");
        String selectionId = file.getAbsolutePath();
        int i = 1;

        while (i < 1000 && (safeName.isEmpty() || this.selections.containsKey(selectionId) || file.exists()))
        {
            name = nameIn + " " + i;
            safeName = FileUtils.generateSafeFileName(name);
            file = new File(dir, safeName + ".json");
            selectionId = file.getAbsolutePath();
            i++;
        }

        AreaSelection selection = new AreaSelection();
        selection.setName(name);
        BlockPos pos = fi.dy.masa.malilib.util.PositionUtils.getEntityBlockPos(this.mc.player);
        selection.createNewSubRegionBox(pos, name);

        this.selections.put(selectionId, selection);
        this.currentSelectionId = selectionId;

        JsonUtils.writeJsonToFile(selection.toJson(), file);

        return this.currentSelectionId;
    }

    public boolean createNewSubRegion(MinecraftClient mc, boolean printMessage)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null && mc.player != null)
        {
            BlockPos pos = BlockPos.ofFloored(mc.player.getPos());

            if (selection.createNewSubRegionBox(pos, selection.getName()) != null)
            {
                if (printMessage)
                {
                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.added_selection_box", posStr);
                }

                return true;
            }
        }

        return false;
    }

    public boolean createNewSubRegionIfDoesntExist(String name, MinecraftClient mc, IMessageConsumer feedback)
    {
        SelectionManager sm = DataManager.getSelectionManager();
        AreaSelection selection = sm.getCurrentSelection();

        if (selection != null && mc.player != null)
        {
            if (selection.getSubRegionBox(name) != null)
            {
                feedback.addMessage(MessageType.ERROR, "litematica.error.area_editor.create_sub_region.exists", name);
                return false;
            }

            BlockPos pos = BlockPos.ofFloored(mc.player.getPos());

            if (selection.createNewSubRegionBox(pos, name) != null)
            {
                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                feedback.addMessage(MessageType.SUCCESS, "litematica.message.added_selection_box", posStr);

                return true;
            }
        }

        return false;
    }

    public boolean createSelectionFromPlacement(File dir, SchematicPlacement placement, String name, IMessageConsumer feedback)
    {
        String safeName = FileUtils.generateSafeFileName(name);

        if (safeName.isEmpty())
        {
            feedback.addMessage(MessageType.ERROR, "litematica.error.area_selection.rename.invalid_safe_file_name", safeName);
            return false;
        }

        File file = new File(dir, safeName + ".json");
        String selectionId = file.getAbsolutePath();
        AreaSelection selection = this.getOrLoadSelectionReadOnly(selectionId);

        if (selection == null)
        {
            selection = AreaSelection.fromPlacement(placement);
            renameSubRegionBoxIfSingle(selection, name);
            selection.setName(name);

            this.selections.put(selectionId, selection);
            this.currentSelectionId = selectionId;

            JsonUtils.writeJsonToFile(selection.toJson(), file);

            return true;
        }

        feedback.addMessage(MessageType.ERROR, "litematica.error.area_selection.create_failed", safeName);

        return false;
    }

    public boolean changeSelection(World world, Entity entity, int maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
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

    public void moveSelectedElement(Direction direction, int amount)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
        {
            area.moveSelectedElement(direction, amount);
        }
    }

    public boolean hasGrabbedElement()
    {
        return this.grabbedElement != null;
    }

    public boolean grabElement(MinecraftClient mc, int maxDistance)
    {
        World world = mc.world;
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        AreaSelection area = this.getCurrentSelection();

        if (area != null && area.getAllSubRegionBoxes().size() > 0)
        {
            RayTraceWrapper trace = RayTraceUtils.getWrappedRayTraceFromEntity(world, entity, maxDistance);

            if (trace.getHitType() == HitType.SELECTION_BOX_CORNER || trace.getHitType() == HitType.SELECTION_BOX_BODY)
            {
                this.changeSelection(area, trace);
                this.grabbedElement = new GrabbedElement(
                        area,
                        trace.getHitSelectionBox(),
                        trace.getHitCorner(),
                        trace.getHitVec(),
                        entity.getCameraPosVec(1f).distanceTo(trace.getHitVec()));
                InfoUtils.printActionbarMessage("litematica.message.grabbed_element_for_moving");
                return true;
            }
        }

        return false;
    }

    public void setPositionOfCurrentSelectionToRayTrace(MinecraftClient mc, Corner corner, boolean moveEntireSelection, double maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
        {
            boolean movingCorner = area.getSelectedSubRegionBox() != null && corner != Corner.NONE;
            boolean movingOrigin = area.isOriginSelected();

            if (movingCorner || movingOrigin)
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                BlockPos pos = RayTraceUtils.getTargetedPosition(mc.world, entity, maxDistance, true);

                if (pos == null)
                {
                    return;
                }

                if (movingOrigin)
                {
                    this.moveSelectionOrigin(area, pos, moveEntireSelection);
                }
                // Moving a corner
                else
                {
                    int cornerIndex = corner.ordinal();

                    if (corner == Corner.CORNER_1 || corner == Corner.CORNER_2)
                    {
                        area.setSelectedSubRegionCornerPos(pos, corner);
                    }

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSubRegionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    InfoUtils.printActionbarMessage("litematica.message.set_selection_box_point", cornerIndex, posStr);
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
            BlockPos old = area.getEffectiveOrigin();
            area.setExplicitOrigin(newOrigin);

            String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
            String posStrNew = String.format("x: %d, y: %d, z: %d", newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.moved_area_origin", posStrOld, posStrNew);
        }
    }

    public void handleCuboidModeMouseClick(MinecraftClient mc, double maxDistance, boolean isRightClick, boolean moveEntireSelection)
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection != null)
        {
            if (selection.isOriginSelected())
            {
                Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
                BlockPos newOrigin = RayTraceUtils.getTargetedPosition(mc.world, entity, maxDistance, true);

                if (newOrigin != null)
                {
                    this.moveSelectionOrigin(selection, newOrigin, moveEntireSelection);
                }
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

    private void resetSelectionToClickedPosition(MinecraftClient mc, double maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null && area.getSelectedSubRegionBox() != null)
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            BlockPos pos = RayTraceUtils.getTargetedPosition(mc.world, entity, maxDistance, true);

            if (pos != null)
            {
                area.setSelectedSubRegionCornerPos(pos, Corner.CORNER_1);
                area.setSelectedSubRegionCornerPos(pos, Corner.CORNER_2);
            }
        }
    }

    private void growSelectionToContainClickedPosition(MinecraftClient mc, double maxDistance)
    {
        AreaSelection sel = this.getCurrentSelection();

        if (sel != null && sel.getSelectedSubRegionBox() != null)
        {
            Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
            BlockPos pos = RayTraceUtils.getTargetedPosition(mc.world, entity, maxDistance, true);

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

                sel.setSelectedSubRegionCornerPos(posMin, Corner.CORNER_1);
                sel.setSelectedSubRegionCornerPos(posMax, Corner.CORNER_2);
            }
        }
    }

    public static void renameSubRegionBoxIfSingle(AreaSelection selection, String newName)
    {
        List<Box> boxes = selection.getAllSubRegionBoxes();

        // If the selection had only one box with the exact same name as the area selection itself,
        // then also rename that box to the new name.
        if (boxes.size() == 1 && boxes.get(0).getName().equals(selection.getName()))
        {
            selection.renameSubRegionBox(selection.getName(), newName);
        }
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

    @Nullable
    public GuiBase getEditGui()
    {
        AreaSelection selection = this.getCurrentSelection();

        if (this.getSelectionMode() == SelectionMode.NORMAL)
        {
            if (selection != null)
            {
                return new GuiAreaSelectionEditorNormal(selection);
            }
            else
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.WARNING, "litematica.error.area_editor.open_gui.no_selection");
                return null;
            }
        }
        else
        {
            return new GuiAreaSelectionEditorSimple(selection);
        }
    }

    public void openEditGui(@Nullable Screen parent)
    {
        GuiBase gui = this.getEditGui();

        if (gui != null)
        {
            gui.setParent(parent);
            GuiBase.openGui(gui);
        }
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

        if (JsonUtils.hasString(obj, "mode"))
        {
            this.mode = SelectionMode.fromString(obj.get("mode").getAsString());
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("mode", new JsonPrimitive(this.mode.name()));

        try
        {
            for (Map.Entry<String, AreaSelection> entry : this.selections.entrySet())
            {
                JsonUtils.writeJsonToFile(entry.getValue().toJson(), new File(entry.getKey()));
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Exception while writing area selections to disk", e);
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
        private final AreaSelection area;
        public final Box grabbedBox;
        public final Box originalBox;
        public final Vec3d grabPosition;
        public final Corner grabbedCorner;
        public double grabDistance;

        private GrabbedElement(AreaSelection area, Box box, Corner corner, Vec3d grabPosition, double grabDistance)
        {
            this.area = area;
            this.grabbedBox = box;
            this.grabbedCorner = corner;
            this.grabPosition = grabPosition;
            this.grabDistance = grabDistance;
            this.originalBox = new Box(box.getPos1(), box.getPos2(), "");
        }

        public void changeGrabDistance(double amount)
        {
            this.grabDistance += amount;
        }

        public void moveElement(Entity entity)
        {
            Vec3d newLookPos = entity.getCameraPosVec(1f).add(entity.getRotationVec(1f).multiply(this.grabDistance));
            Vec3d change = newLookPos.subtract(this.grabPosition);

            if ((this.grabbedCorner == Corner.NONE || this.grabbedCorner == Corner.CORNER_1) && this.grabbedBox.getPos1() != null)
            {
                BlockPos pos = this.originalBox.getPos1().add((int)Math.floor(change.x), (int)Math.floor(change.y), (int)Math.floor(change.z));
                this.area.setSubRegionCornerPos(this.grabbedBox, Corner.CORNER_1, pos);
            }

            if ((this.grabbedCorner == Corner.NONE || this.grabbedCorner == Corner.CORNER_2) && this.grabbedBox.getPos2() != null)
            {
                BlockPos pos = this.originalBox.getPos2().add((int)Math.floor(change.x), (int)Math.floor(change.y), (int)Math.floor(change.z));
                this.area.setSubRegionCornerPos(this.grabbedBox, Corner.CORNER_2, pos);
            }
        }
    }
}
