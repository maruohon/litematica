package litematica.selection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import malilib.gui.BaseScreen;
import malilib.gui.util.GuiUtils;
import malilib.overlay.message.MessageDispatcher;
import malilib.overlay.message.MessageOutput;
import malilib.util.FileNameUtils;
import malilib.util.FileUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.Direction;
import malilib.util.position.PositionUtils;
import litematica.Litematica;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.MultiRegionModeAreaEditorScreen;
import litematica.gui.SimpleModeAreaEditorScreen;
import litematica.schematic.placement.SchematicPlacement;
import litematica.schematic.projects.SchematicProject;
import litematica.util.RayTraceUtils;
import litematica.util.RayTraceUtils.RayTraceWrapper;
import litematica.util.RayTraceUtils.RayTraceWrapper.HitType;

public class AreaSelectionManager
{
    protected final Map<String, AreaSelection> selections = new HashMap<>();
    protected final Map<String, AreaSelection> readOnlySelections = new HashMap<>();
    protected AreaSelectionType mode = AreaSelectionType.SIMPLE;
    @Nullable protected String currentSelectionId;

    public AreaSelectionType getSelectionMode()
    {
        if (DataManager.getSchematicProjectsManager().hasProjectOpen())
        {
            SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();
            return project != null ? project.getSelectionMode() : AreaSelectionType.SIMPLE;
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
                MessageDispatcher.warning().screenOrActionbar()
                        .translate("litematica.error.schematic_projects.in_projects_mode_but_no_project_open");
            }
        }
        else
        {
            this.mode = this.mode == AreaSelectionType.MULTI_REGION ? AreaSelectionType.SIMPLE : AreaSelectionType.MULTI_REGION;
        }
    }

    @Nullable
    public String getCurrentSelectionId()
    {
        return this.mode == AreaSelectionType.MULTI_REGION ? this.currentSelectionId : null;
    }

    @Nullable
    public String getCurrentMultiRegionSelectionId()
    {
        return this.currentSelectionId;
    }

    public boolean hasMultiRegionSelection()
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
        SchematicProject project = DataManager.getSchematicProjectsManager().getCurrentProject();

        if (project != null)
        {
            return project.getSelection();
        }

        return this.getSelection(this.currentSelectionId);
    }

    @Nullable
    public AreaSelection getSelection(@Nullable String selectionId)
    {
        if (this.mode == AreaSelectionType.SIMPLE)
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
    protected AreaSelection tryLoadSelectionFromFile(String selectionId)
    {
        return tryLoadSelectionFromFile(Paths.get(selectionId));
    }

    @Nullable
    public static AreaSelection tryLoadSelectionFromFile(Path file)
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
            if (selectionId.equals(this.currentSelectionId))
            {
                this.currentSelectionId = null;
            }

            Path file = Paths.get(selectionId);

            if (Files.exists(file))
            {
                FileUtils.delete(file);
            }

            return true;
        }

        return false;
    }

    public boolean renameSelection(String selectionId, String newName, MessageOutput output)
    {
        Path dir = Paths.get(selectionId);
        dir = dir.getParent();

        return this.renameSelection(dir, selectionId, newName, output);
    }

    public boolean renameSelection(Path dir, String selectionId, String newName, MessageOutput output)
    {
        return this.renameSelection(dir, selectionId, newName, false, output);
    }

    public boolean renameSelection(Path dir, String selectionId, String newName, boolean copy, MessageOutput output)
    {
        Path file = Paths.get(selectionId);

        if (Files.isRegularFile(file))
        {
            String newFileName = FileNameUtils.generateSafeFileName(newName);

            if (newFileName.isEmpty())
            {
                String key = "litematica.error.area_selection.rename.invalid_safe_file_name";
                MessageDispatcher.error().type(output).translate(key, newFileName);
                return false;
            }

            Path newFile = dir.resolve(newFileName + ".json");

            if (Files.exists(newFile) == false && (copy || FileUtils.move(file, newFile)))
            {
                String newId = newFile.toAbsolutePath().toString();
                AreaSelection selection;

                if (copy)
                {
                    try
                    {
                        Files.copy(file, newFile);
                    }
                    catch (Exception e)
                    {
                        MessageDispatcher.error().console(e).type(output)
                                .translate("litematica.error.area_selection.copy_failed");
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
                MessageDispatcher.error().type(output)
                        .translate("litematica.error.area_selection.rename.already_exists",
                                   newFile.getFileName().toString());
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
     * Creates a new area selection and returns its selectionId
     */
    public String createNewSelection(Path dir, final String nameIn)
    {
        String name = nameIn;
        String safeName = FileNameUtils.generateSafeFileName(name);
        Path file = dir.resolve(safeName + ".json");
        String selectionId = file.toAbsolutePath().toString();
        int i = 1;

        while (i < 1000 && (safeName.isEmpty() || this.selections.containsKey(selectionId) || Files.exists(file)))
        {
            name = nameIn + " " + i;
            safeName = FileNameUtils.generateSafeFileName(name);
            file = dir.resolve(safeName + ".json");
            selectionId = file.toAbsolutePath().toString();
            i++;
        }

        AreaSelection selection = new AreaSelection();
        selection.setName(name);
        BlockPos pos = EntityWrap.getCameraEntityBlockPos();
        selection.createNewSelectionBox(pos, name);

        this.selections.put(selectionId, selection);
        this.currentSelectionId = selectionId;

        JsonUtils.writeJsonToFile(selection.toJson(), file);

        return this.currentSelectionId;
    }

    public boolean createNewSubRegion(boolean printMessage)
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection != null)
        {
            BlockPos pos = EntityWrap.getCameraEntityBlockPos();

            if (selection.createNewSelectionBox(pos, selection.getName()) != null)
            {
                if (printMessage)
                {
                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageDispatcher.success().screenOrActionbar()
                            .translate("litematica.message.added_selection_box", posStr);
                }

                return true;
            }
        }

        return false;
    }

    public boolean createNewSubRegionIfNotExists(String name)
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection != null)
        {
            if (selection.getSelectionBox(name) != null)
            {
                MessageDispatcher.error().translate("litematica.error.area_editor.create_sub_region.exists", name);
                return false;
            }

            BlockPos pos = EntityWrap.getCameraEntityBlockPos();

            if (selection.createNewSelectionBox(pos, name) != null)
            {
                String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                MessageDispatcher.success().translate("litematica.message.added_selection_box", posStr);

                return true;
            }
        }

        return false;
    }

    public boolean createSelectionFromPlacement(Path dir, SchematicPlacement placement, String name)
    {
        String safeName = FileNameUtils.generateSafeFileName(name);

        if (safeName.isEmpty())
        {
            MessageDispatcher.error().translate("litematica.error.area_selection.rename.invalid_safe_file_name", safeName);
            return false;
        }

        Path file = dir.resolve(safeName + ".json");
        String selectionId = file.toAbsolutePath().toString();
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

        MessageDispatcher.error().translate("litematica.error.area_selection.create_failed", safeName);

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
                area.clearCornerSelectionOfSelectedBox();
                area.setSelectedSelectionBox(null);
                area.setOriginSelected(false);
                return true;
            }
        }

        return false;
    }

    protected void changeSelection(AreaSelection area, RayTraceWrapper trace)
    {
        area.clearCornerSelectionOfSelectedBox();

        if (trace.getHitType() == HitType.SELECTION_BOX_CORNER || trace.getHitType() == HitType.SELECTION_BOX_BODY)
        {
            SelectionBox box = trace.getHitSelectionBox();
            area.setSelectedSelectionBox(box.getName());
            area.setOriginSelected(false);
            box.setSelectedCorner(trace.getHitCorner());
        }
        else if (trace.getHitType() == HitType.SELECTION_ORIGIN)
        {
            area.setSelectedSelectionBox(null);
            area.setOriginSelected(true);
        }
    }

    public boolean hasSelectedElement()
    {
        AreaSelection area = this.getCurrentSelection();
        return area != null && (area.getSelectedSelectionBox() != null || area.isOriginSelected());
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

    public void setPositionOfCurrentSelectionToRayTrace(BoxCorner corner, boolean moveEntireSelection, double maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null)
        {
            boolean movingCorner = area.getSelectedSelectionBox() != null && corner != BoxCorner.NONE;
            boolean movingOrigin = area.isOriginSelected();

            if (movingCorner || movingOrigin)
            {
                Entity entity = GameWrap.getCameraEntity();
                // TODO: Add a new ray trace method that internally fetches the entity, either the camera or the player, depending on some config option
                BlockPos pos = RayTraceUtils.getTargetedPosition(GameWrap.getClientWorld(), entity, maxDistance, true);

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

                    if (corner == BoxCorner.CORNER_1 || corner == BoxCorner.CORNER_2)
                    {
                        area.setSelectedSelectionBoxCornerPos(pos, corner);
                    }

                    if (Configs.Generic.CHANGE_SELECTED_CORNER.getBooleanValue())
                    {
                        area.getSelectedSelectionBox().setSelectedCorner(corner);
                    }

                    String posStr = String.format("x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ());
                    MessageDispatcher.generic().customHotbar().translate("litematica.message.set_selection_box_point",
                                                                         cornerIndex, posStr);
                }
            }
        }
    }

    public void moveSelectionOrigin(AreaSelection area, BlockPos newOrigin, boolean moveEntireSelection)
    {
        if (moveEntireSelection)
        {
            area.moveEntireAreaSelectionTo(newOrigin, true);
        }
        else
        {
            BlockPos old = area.getEffectiveOrigin();
            area.setManualOrigin(newOrigin);

            String posStrOld = String.format("x: %d, y: %d, z: %d", old.getX(), old.getY(), old.getZ());
            String posStrNew = String.format("x: %d, y: %d, z: %d", newOrigin.getX(), newOrigin.getY(), newOrigin.getZ());
            MessageDispatcher.success().screenOrActionbar()
                    .translate("litematica.message.moved_area_origin", posStrOld, posStrNew);
        }
    }

    public void handleExpandModeMouseClick(double maxDistance, boolean isRightClick, boolean moveEntireSelection)
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection != null)
        {
            if (selection.isOriginSelected())
            {
                Entity entity = GameWrap.getCameraEntity();
                BlockPos newOrigin = RayTraceUtils.getTargetedPosition(GameWrap.getClientWorld(), entity, maxDistance, true);

                if (newOrigin != null)
                {
                    this.moveSelectionOrigin(selection, newOrigin, moveEntireSelection);
                }
            }
            // Right click in Cuboid mode: Reset the area to the clicked position
            else if (isRightClick)
            {
                this.resetSelectionToClickedPosition(maxDistance);
            }
            // Left click in Cuboid mode: Grow the selection to contain each clicked position
            else
            {
                this.growSelectionToContainClickedPosition(maxDistance);
            }
        }
    }

    protected void resetSelectionToClickedPosition(double maxDistance)
    {
        AreaSelection area = this.getCurrentSelection();

        if (area != null && area.getSelectedSelectionBox() != null)
        {
            Entity entity = GameWrap.getCameraEntity();
            BlockPos pos = RayTraceUtils.getTargetedPosition(GameWrap.getClientWorld(), entity, maxDistance, true);

            if (pos != null)
            {
                area.setSelectedSelectionBoxCornerPos(pos, BoxCorner.CORNER_1);
                area.setSelectedSelectionBoxCornerPos(pos, BoxCorner.CORNER_2);
            }
        }
    }

    protected void growSelectionToContainClickedPosition(double maxDistance)
    {
        AreaSelection sel = this.getCurrentSelection();

        if (sel != null && sel.getSelectedSelectionBox() != null)
        {
            Entity entity = GameWrap.getCameraEntity();
            BlockPos pos = RayTraceUtils.getTargetedPosition(GameWrap.getClientWorld(), entity, maxDistance, true);

            if (pos != null)
            {
                CornerDefinedBox box = sel.getSelectedSelectionBox();
                BlockPos pos1 = box.getCorner1();
                BlockPos pos2 = box.getCorner2();
                BlockPos posMin = PositionUtils.getMinCorner(pos, pos1, pos2);
                BlockPos posMax = PositionUtils.getMaxCorner(pos, pos1, pos2);

                sel.setSelectedSelectionBoxCornerPos(posMin, BoxCorner.CORNER_1);
                sel.setSelectedSelectionBoxCornerPos(posMax, BoxCorner.CORNER_2);
            }
        }
    }

    public static void renameSubRegionBoxIfSingle(AreaSelection selection, String newName)
    {
        List<SelectionBox> boxes = selection.getAllSelectionBoxes();

        // If the selection had only one box with the exact same name as the area selection itself,
        // then also rename that box to the new name.
        if (boxes.size() == 1 && boxes.get(0).getName().equals(selection.getName()))
        {
            selection.renameSelectionBox(selection.getName(), newName);
        }
    }

    public void clear()
    {
        this.mode = Configs.Generic.DEFAULT_AREA_SELECTION_MODE.getValue();
        this.currentSelectionId = null;
        this.selections.clear();
        this.readOnlySelections.clear();
    }

    @Nullable
    public BaseScreen getEditGui()
    {
        AreaSelection selection = this.getCurrentSelection();

        if (selection == null)
        {
            MessageDispatcher.warning().screenOrActionbar().translate("litematica.error.area_editor.open_gui.no_selection");
            return null;
        }

        if (this.getSelectionMode() == AreaSelectionType.MULTI_REGION)
        {
            return new MultiRegionModeAreaEditorScreen(selection);
        }
        else if (this.getSelectionMode() == AreaSelectionType.SIMPLE)
        {
            return new SimpleModeAreaEditorScreen(selection);
        }

        return null;
    }

    public void openAreaEditorScreenWithParent()
    {
        this.openEditGui(GuiUtils.getCurrentScreen());
    }

    public void openEditGui(@Nullable GuiScreen parent)
    {
        BaseScreen gui = this.getEditGui();

        if (gui != null)
        {
            gui.setParent(parent);
            BaseScreen.openScreen(gui);
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
            String name = obj.get("mode").getAsString();
            this.mode = AreaSelectionType.findValueByName(name, AreaSelectionType.VALUES);
        }
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.addProperty("mode", this.mode.getName());

        try
        {
            for (Map.Entry<String, AreaSelection> entry : this.selections.entrySet())
            {
                JsonUtils.writeJsonToFile(entry.getValue().toJson(), Paths.get(entry.getKey()));
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.warn("Exception while writing area selections to file", e);
        }

        AreaSelection current = this.currentSelectionId != null ? this.selections.get(this.currentSelectionId) : null;

        // Clear the loaded selections, except for the currently selected one
        this.selections.clear();
        this.readOnlySelections.clear();

        if (current != null)
        {
            obj.addProperty("current", this.currentSelectionId);
            this.selections.put(this.currentSelectionId, current);
        }

        return obj;
    }
}
