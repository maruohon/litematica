package fi.dy.masa.litematica.schematic.versioning;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.ICompletionListener;
import fi.dy.masa.litematica.scheduler.TaskScheduler;
import fi.dy.masa.litematica.scheduler.tasks.TaskSaveSchematic;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.AreaSelectionSimple;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.selection.SelectionMode;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class SchematicProject
{
    private final List<SchematicVersion> versions = new ArrayList<>();
    private final File directory;
    private File projectFile;
    private BlockPos origin = BlockPos.ORIGIN;
    private String projectName = "unnamed";
    private AreaSelection selection = new AreaSelection();
    private AreaSelectionSimple selectionSimple = new AreaSelectionSimple(true);
    private int currentVersionId = -1;
    private int lastCheckedOutVersion = -1;
    private boolean saveInProgress;
    private boolean dirty;
    @Nullable
    private SchematicVersion currentVersion;
    @Nullable
    private SchematicPlacement currentPlacement;

    public SchematicProject(File directory, File projectFile)
    {
        this.directory = directory;
        this.projectFile = projectFile;
    }

    public File getDirectory()
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

    public void setName(String name)
    {
        // TODO should renaming be allowed? Should the schematics and versions be renamed/updated when renamed?
        File newFile = new File(this.directory, name + ".json");

        if (newFile.exists() == false)
        {
            try
            {
                FileUtils.moveFile(this.projectFile, newFile);
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
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.failed_to_rename_project_file_exception", newFile.getAbsolutePath());
            }
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.failed_to_rename_project_file_exists", name);
        }
    }

    public void setOrigin(BlockPos origin)
    {
        this.getSelection().moveEntireSelectionTo(origin, false);
        this.origin = origin;

        if (this.currentVersion != null)
        {
            BlockPos areaPosition = this.origin.add(this.currentVersion.getAreaOffset());

            if (this.currentPlacement != null)
            {
                this.currentPlacement.setOrigin(areaPosition, InfoUtils.INFO_MESSAGE_CONSUMER);
            }
        }

        this.dirty = true;
    }

    public File getProjectFile()
    {
        return this.projectFile;
    }

    public BlockPos getOrigin()
    {
        return this.origin;
    }

    @Nullable
    public AreaSelection getSelection()
    {
        if (DataManager.getSelectionManager().getSelectionMode() == SelectionMode.SIMPLE)
        {
            return this.selectionSimple;
        }

        return this.selection;
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
                    this.currentPlacement = SchematicPlacement.createFor(schematic, areaPosition, version.getName(), true, true);
                    this.currentPlacement.setShouldBeSaved(false);
                    DataManager.getSchematicPlacementManager().addSchematicPlacement(this.currentPlacement, null);
                }
                else
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.failed_to_load_schematic");
                }

                this.lastCheckedOutVersion = this.currentVersionId;
            }
        }
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
            return this.switchVersion(this.currentVersionId + amount);
        }

        return false;
    }

    public boolean switchVersion(int version)
    {
        if (version != this.currentVersionId && version >= 0 && version < this.versions.size())
        {
            this.currentVersionId = version;
            this.currentVersion = this.versions.get(this.currentVersionId);
            this.createAndAddPlacement();
            this.dirty = true;
            return true;
        }

        return false;
    }

    public boolean switchVersion(SchematicVersion version)
    {
        int index = this.versions.indexOf(version);

        if (index >= 0 && version != this.getCurrentVersion())
        {
            return this.switchVersion(index);
        }

        return false;
    }

    public boolean commitNewVersion(String name)
    {
        if (this.checkCanSaveOrPrintError())
        {
            Minecraft mc = Minecraft.getMinecraft();
            String author = mc.player.getName();
            String fileName = this.getNextFileName();

            AreaSelection selection = this.getSelection();
            LitematicaSchematic schematic = LitematicaSchematic.createEmptySchematic(selection, author);
            schematic.getMetadata().setName(name);
            BlockPos areaOffset = selection.getEffectiveOrigin().subtract(this.origin);
            SaveCompletionListener listener = new SaveCompletionListener(name, fileName, areaOffset);

            TaskSaveSchematic task = new TaskSaveSchematic(this.directory, fileName, schematic, selection.copy(), true, false);
            task.setCompletionListener(listener);
            TaskScheduler.getInstance().scheduleTask(task, 2);
            this.saveInProgress = true;
            this.dirty = true;

            return true;
        }

        return false;
    }

    private String getNextFileName()
    {
        String nameBase = this.projectName + "_";
        int version = 1;
        int failsafe = 10000000;

        while (failsafe-- > 0)
        {
            String name = nameBase + String.format("%05d", version);
            File file = new File(this.directory, name + LitematicaSchematic.FILE_EXTENSION);

            if (file.exists() == false)
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
        this.lastCheckedOutVersion = -1;
        this.currentVersionId = -1;
        //this.currentVersionNumber = 0;
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

        obj.add("directory", new JsonPrimitive(this.directory.getAbsolutePath()));
        obj.add("name", new JsonPrimitive(this.projectName));
        obj.add("origin", JsonUtils.blockPosToJson(this.origin));
        obj.add("current_version_id", new JsonPrimitive(this.currentVersionId));
        obj.add("selection_normal", this.selection.toJson());
        obj.add("selection_simple", this.selectionSimple.toJson());

        JsonArray arr = new JsonArray();

        for (int i = 0; i < this.versions.size(); ++i)
        {
            arr.add(this.versions.get(i).toJson());
        }

        if (arr.size() > 0)
        {
            obj.add("versions", arr);
        }

        return obj;
    }

    @Nullable
    public static SchematicProject fromJson(JsonObject obj, File projectFile)
    {
        BlockPos origin = JsonUtils.blockPosFromJson(obj, "origin");

        if (JsonUtils.hasString(obj, "directory") &&
            JsonUtils.hasString(obj, "name") &&
            JsonUtils.hasInteger(obj, "current_version_id") &&
            origin != null)
        {
            SchematicProject project = new SchematicProject(new File(JsonUtils.getString(obj, "directory")), projectFile);
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

            if (JsonUtils.hasArray(obj, "versions"))
            {
                JsonArray arr = obj.get("versions").getAsJsonArray();

                for (int i = 0; i < arr.size(); ++i)
                {
                    JsonElement el = arr.get(i);

                    if (el.isJsonObject())
                    {
                        SchematicVersion version = SchematicVersion.fromJson(el.getAsJsonObject());

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

            project.switchVersion(id);
            project.dirty = false;

            return project;
        }

        return null;
    }

    boolean checkCanSaveOrPrintError()
    {
        if (this.saveInProgress)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.save_already_in_progress");
            return false;
        }

        if (this.directory == null || this.directory.exists() == false || this.directory.isDirectory() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.invalid_project_directory");
            return false;
        }

        if (this.getSelection().getAllSubRegionBoxes().size() == 0)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.empty_selection");
            return false;
        }

        if (Minecraft.getMinecraft().player == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.null_player");
            return false;
        }

        return true;
    }

    private class SaveCompletionListener implements ICompletionListener
    {
        private final String name;
        private final String fileName;
        private final BlockPos areaOffset;
        private final int version;

        private SaveCompletionListener(String name, String fileName, BlockPos areaOffset)
        {
            this.name = name;
            this.fileName = fileName;
            this.areaOffset = areaOffset;
            this.version = SchematicProject.this.versions.size() + 1;
        }

        @Override
        public void onTaskCompleted()
        {
            SchematicVersion version = new SchematicVersion(this.name, this.fileName, this.areaOffset, this.version, System.currentTimeMillis());
            SchematicProject.this.versions.add(version);
            SchematicProject.this.switchVersion(SchematicProject.this.versions.size() - 1);
            SchematicProject.this.saveInProgress = false;

            InfoUtils.showGuiOrInGameMessage(MessageType.SUCCESS, "litematica.message.schematic_versioning.version_saved", this.version, this.name);
        }
    }
}
