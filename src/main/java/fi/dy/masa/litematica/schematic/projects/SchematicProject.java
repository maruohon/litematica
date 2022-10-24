package fi.dy.masa.litematica.schematic.projects;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import malilib.listener.TaskCompletionListener;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.FileUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.game.wrap.GameUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.util.SchematicCreationUtils;
import fi.dy.masa.litematica.schematic.util.SchematicPlacingUtils;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.litematica.task.CreateSchematicTask;
import fi.dy.masa.litematica.util.ToolUtils;

public class SchematicProject
{
    private final List<SchematicVersion> versions = new ArrayList<>();
    private final Path directory;
    private Path projectFile;
    private BlockPos origin = BlockPos.ORIGIN;
    private String projectName = "unnamed";
    private AreaSelection selection = new AreaSelection();
    private AreaSelection lastSeenArea = new AreaSelection();
    private AreaSelectionSimple selectionSimple = new AreaSelectionSimple(true);
    private SelectionMode selectionMode = SelectionMode.SIMPLE;
    private int currentVersionId = -1;
    private int lastCheckedOutVersion = -1;
    private boolean saveInProgress;
    private boolean dirty;
    @Nullable
    private SchematicPlacement currentPlacement;

    public SchematicProject(Path directory, Path projectFile)
    {
        this.directory = directory;
        this.projectFile = projectFile;
    }

    public Path getDirectory()
    {
        return this.directory;
    }

    public String getName()
    {
        return this.projectName;
    }

    public int getVersionCount()
    {
        return this.versions.size();
    }

    public int getCurrentVersionId()
    {
        return this.currentVersionId;
    }

    public String getCurrentVersionName()
    {
        SchematicVersion currentVersion = this.getCurrentVersion();
        return currentVersion != null ? currentVersion.getName() : this.getSelection().getName();
    }

    public void setName(String name)
    {
        Path newFile = this.directory.resolve(name + ".json");

        if (Files.exists(newFile) == false)
        {
            try
            {
                if (Files.exists(this.projectFile))
                {
                    FileUtils.move(this.projectFile, newFile);
                }

                this.projectName = name;
                this.projectFile = newFile;
                this.selection.setName(name);
                this.selectionSimple.setName(name);
                SelectionManager.renameSubRegionBoxIfSingle(this.selection, name);
                SelectionManager.renameSubRegionBoxIfSingle(this.selectionSimple, name);
                this.dirty = true;
            }
            catch (Exception e)
            {
                String key = "litematica.error.schematic_projects.failed_to_rename_project_file_exception";
                MessageDispatcher.error().translate(key, newFile.toAbsolutePath().toString());
            }
        }
        else
        {
            String key = "litematica.error.schematic_projects.failed_to_rename_project_file_exists";
            MessageDispatcher.error().translate(key, name);
        }
    }

    public void setOrigin(BlockPos origin)
    {
        BlockPos offset = this.selection.getEffectiveOrigin().subtract(this.origin);
        this.selection.moveEntireSelectionTo(origin.add(offset), false);

        offset = this.selectionSimple.getEffectiveOrigin().subtract(this.origin);
        this.selectionSimple.moveEntireSelectionTo(origin.add(offset), false);

        // Forget the old last seen area, it will be invalid after moving the entire project
        this.lastSeenArea = new AreaSelection();

        this.origin = origin;
        SchematicVersion currentVersion = this.getCurrentVersion();

        if (currentVersion != null)
        {
            BlockPos areaPosition = this.origin.add(currentVersion.getAreaOffset());

            if (this.currentPlacement != null)
            {
                DataManager.getSchematicPlacementManager().setOrigin(this.currentPlacement, areaPosition);
            }
        }

        this.dirty = true;
    }

    public Path getProjectFile()
    {
        return this.projectFile;
    }

    public BlockPos getOrigin()
    {
        return this.origin;
    }

    public AreaSelection getSelection()
    {
        if (this.selectionMode == SelectionMode.SIMPLE)
        {
            return this.selectionSimple;
        }

        return this.selection;
    }

    public SelectionMode getSelectionMode()
    {
        return this.selectionMode;
    }

    public void switchSelectionMode()
    {
        this.selectionMode = this.selectionMode == SelectionMode.MULTI_REGION ? SelectionMode.SIMPLE : SelectionMode.MULTI_REGION;
        this.dirty = true;
    }

    public ImmutableList<SchematicVersion> getAllVersions()
    {
        return ImmutableList.copyOf(this.versions);
    }

    @Nullable
    public SchematicVersion getCurrentVersion()
    {
        if (this.currentVersionId >= 0 && this.currentVersionId < this.versions.size())
        {
            return this.versions.get(this.currentVersionId);
        }

        return null;
    }

    @Nullable
    public SchematicPlacement getCurrentPlacement()
    {
        return this.currentPlacement;
    }

    private void createAndAddPlacement()
    {
        SchematicVersion version = this.getCurrentVersion();

        if (version != null)
        {
            if (this.currentVersionId != this.lastCheckedOutVersion)
            {
                this.removeCurrentPlacement();

                LitematicaSchematic schematic = LitematicaSchematic.createFromFile(this.directory, version.getFileName());

                if (schematic != null)
                {
                    BlockPos areaPosition = this.origin.add(version.getAreaOffset());
                    this.currentPlacement = SchematicPlacement.createFor(schematic, areaPosition, version.getName(), true, false);
                    this.currentPlacement.setShouldBeSaved(false);
                    DataManager.getSchematicPlacementManager().addSchematicPlacement(this.currentPlacement, false);

                    long time = schematic.getMetadata().getTimeCreated();

                    if (time != version.getTimeStamp())
                    {
                        version = new SchematicVersion(this, version.getName(), version.getFileName(),
                                                       version.getAreaOffset(), version.getVersion(), time);
                        this.versions.set(this.currentVersionId, version);
                        this.dirty = true;
                    }
                }
                else
                {
                    MessageDispatcher.error().translate("litematica.error.schematic_projects.failed_to_load_schematic");
                }

                this.lastCheckedOutVersion = this.currentVersionId;
            }
        }
    }

    public void pasteToWorld()
    {
        if (this.currentPlacement != null)
        {
            if (GameUtils.getClientPlayer() == null || GameUtils.isCreativeMode() == false)
            {
                MessageDispatcher.error().translate("litematica.error.generic.creative_mode_only");
                return;
            }

            this.cacheCurrentAreaFromPlacement();

            TaskCompletionListener completionListener = () -> SchematicPlacingUtils.pastePlacementToWorld(this.currentPlacement, false);
            ToolUtils.deleteSelectionVolumes(this.lastSeenArea, true, completionListener);
        }
    }

    public void deleteLastSeenArea()
    {
        ToolUtils.deleteSelectionVolumes(this.lastSeenArea, true);
    }

    public void removeCurrentPlacement()
    {
        if (this.currentPlacement != null)
        {
            DataManager.getSchematicPlacementManager().removeSchematicPlacement(this.currentPlacement);
        }
    }

    public boolean cycleVersion(int amount)
    {
        if (this.currentVersionId >= 0)
        {
            return this.switchVersion(this.currentVersionId + amount, true);
        }

        return false;
    }

    public boolean switchVersion(int version, boolean createPlacement)
    {
        if (version != this.currentVersionId && version >= 0 && version < this.versions.size())
        {
            this.currentVersionId = version;
            this.dirty = true;

            if (createPlacement)
            {
                this.createAndAddPlacement();
            }

            return true;
        }

        return false;
    }

    public boolean switchVersion(SchematicVersion version, boolean createPlacement)
    {
        int index = this.versions.indexOf(version);

        if (index >= 0 && version != this.getCurrentVersion())
        {
            return this.switchVersion(index, createPlacement);
        }

        return false;
    }

    private void cacheCurrentAreaFromPlacement()
    {
        if (this.currentPlacement != null)
        {
            this.lastSeenArea = AreaSelection.fromPlacement(this.currentPlacement);
            this.dirty = true;
        }
    }

    public boolean commitNewVersion(String name)
    {
        if (this.checkCanSaveOrPrintError())
        {
            String fileName = this.getNextFileName();
            AreaSelection selection = this.getSelection();
            BlockPos areaOffset = selection.getEffectiveOrigin().subtract(this.origin);

            LitematicaSchematic schematic = SchematicCreationUtils.createEmptySchematic(selection);
            CreateSchematicTask task = new CreateSchematicTask(schematic, selection.copy(), false,
                () -> this.writeSchematicToFileAndAddVersion(schematic, fileName, name, areaOffset));

            TaskScheduler.getServerInstanceIfExistsOrClient().scheduleTask(task, 2);

            this.saveInProgress = true;
            this.dirty = true;
            this.saveToFile();

            return true;
        }

        return false;
    }

    protected void writeSchematicToFileAndAddVersion(ISchematic schematic, String fileName, String name, Vec3i areaOffset)
    {
        SchematicCreationUtils.setSchematicMetadataOnCreation(schematic, name);

        if (schematic.writeToFile(this.directory, fileName, false))
        {
            int versionNumber = this.versions.size() + 1;
            SchematicVersion version = new SchematicVersion(this, name, fileName, areaOffset,
                                                            versionNumber, System.currentTimeMillis());
            this.versions.add(version);
            this.switchVersion(this.versions.size() - 1, true);
            this.cacheCurrentAreaFromPlacement();
            this.saveInProgress = false;

            MessageDispatcher.success("litematica.message.schematic_projects.version_saved", version, name);
        }
    }

    private String getNextFileName()
    {
        String nameBase = this.projectName + "_";
        int version = 1;
        int failsafe = 10000;
        // FIXME wtf is this shit

        while (failsafe-- > 0)
        {
            String name = nameBase + String.format("%05d", version);
            Path file = this.directory.resolve(name + LitematicaSchematic.FILE_NAME_EXTENSION);

            if (Files.exists(file) == false)
            {
                return name;
            }

            ++version;
        }

        return nameBase + "error";
    }

    public void clear()
    {
        this.origin = BlockPos.ORIGIN;
        this.versions.clear();
        this.selection = new AreaSelection();
        this.selectionSimple = new AreaSelectionSimple(true);
        this.lastSeenArea = new AreaSelection();
        this.lastCheckedOutVersion = -1;
        this.currentVersionId = -1;
        this.saveInProgress = false;
    }

    public boolean saveToFile()
    {
        if (this.dirty == false || JsonUtils.writeJsonToFile(this.toJson(), this.projectFile))
        {
            this.dirty = false;
            return true;
        }

        return false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("name", new JsonPrimitive(this.projectName));
        obj.add("origin", JsonUtils.blockPosToJson(this.origin));
        obj.add("current_version_id", new JsonPrimitive(this.currentVersionId));
        obj.add("selection_normal", this.selection.toJson());
        obj.add("selection_simple", this.selectionSimple.toJson());
        obj.add("last_seen_area", this.lastSeenArea.toJson());
        obj.add("selection_mode", new JsonPrimitive(this.selectionMode.getName()));

        JsonArray arr = new JsonArray();

        for (SchematicVersion version : this.versions)
        {
            arr.add(version.toJson());
        }

        if (arr.size() > 0)
        {
            obj.add("versions", arr);
        }

        return obj;
    }

    @Nullable
    public static SchematicProject fromJson(JsonObject obj, Path projectFile, boolean createPlacement)
    {
        BlockPos origin = JsonUtils.blockPosFromJson(obj, "origin");

        if (JsonUtils.hasString(obj, "name") && JsonUtils.hasInteger(obj, "current_version_id") && origin != null)
        {
            projectFile = projectFile.toAbsolutePath();
            SchematicProject project = new SchematicProject(projectFile.getParent(), projectFile);
            project.projectName = JsonUtils.getString(obj, "name");
            project.origin = origin;

            if (JsonUtils.hasObject(obj, "selection_normal"))
            {
                project.selection = AreaSelection.fromJson(JsonUtils.getNestedObject(obj, "selection_normal", false));
            }

            if (JsonUtils.hasObject(obj, "selection_simple"))
            {
                project.selectionSimple = AreaSelectionSimple.fromJson(JsonUtils.getNestedObject(obj, "selection_simple", false));
            }

            if (JsonUtils.hasObject(obj, "last_seen_area"))
            {
                project.lastSeenArea = AreaSelection.fromJson(JsonUtils.getNestedObject(obj, "last_seen_area", false));
            }

            if (JsonUtils.hasString(obj, "selection_mode"))
            {
                String name = JsonUtils.getString(obj, "selection_mode");
                project.selectionMode = SelectionMode.findValueByName(name, SelectionMode.VALUES);
            }

            if (JsonUtils.hasArray(obj, "versions"))
            {
                JsonArray arr = obj.get("versions").getAsJsonArray();

                for (int i = 0; i < arr.size(); ++i)
                {
                    JsonElement el = arr.get(i);

                    if (el.isJsonObject())
                    {
                        SchematicVersion version = SchematicVersion.fromJson(el.getAsJsonObject(), project);

                        if (version != null)
                        {
                            project.versions.add(version);
                        }
                    }
                }
            }

            int id = project.versions.size() - 1;

            if (JsonUtils.hasInteger(obj, "current_version_id"))
            {
                int tmp = JsonUtils.getInteger(obj, "current_version_id");

                if (tmp >= 0 && tmp < project.versions.size())
                {
                    id = tmp;
                }
            }

            project.switchVersion(id, createPlacement);

            project.dirty = false;

            return project;
        }

        return null;
    }

    boolean checkCanSaveOrPrintError()
    {
        if (this.saveInProgress)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_projects.save_already_in_progress");
            return false;
        }

        if (this.directory == null || Files.isDirectory(this.directory) == false)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_projects.invalid_project_directory");
            return false;
        }

        if (this.getSelection().getAllSubRegionBoxes().size() == 0)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_projects.empty_selection");
            return false;
        }

        if (GameUtils.getClientPlayer() == null)
        {
            MessageDispatcher.error().translate("litematica.error.schematic_projects.null_player");
            return false;
        }

        return true;
    }
}
