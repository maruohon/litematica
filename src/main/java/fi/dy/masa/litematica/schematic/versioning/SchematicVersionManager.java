package fi.dy.masa.litematica.schematic.versioning;

import java.io.File;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

public class SchematicVersionManager
{
    //private static final Pattern PATTERN_NAME_NUMBER = Pattern.compile("(.*)([0-9]+)$");

    @Nullable
    private SchematicProject currentProject;

    @Nullable
    public SchematicProject getCurrentProject()
    {
        return this.currentProject;
    }

    public void createNewProject(File dir, String projectName)
    {
        this.closeCurrentProject();

        this.currentProject = new SchematicProject(dir);
        this.currentProject.setName(projectName);
        this.currentProject.setOrigin(new BlockPos(Minecraft.getMinecraft().player));
        this.currentProject.saveToFile();
    }

    public boolean openProject(File projectFile)
    {
        this.closeCurrentProject();

        if (projectFile.getName().endsWith(".json") && projectFile.exists() && projectFile.isFile() && projectFile.canRead())
        {
            JsonElement el = JsonUtils.parseJsonFile(projectFile);

            if (el != null && el.isJsonObject())
            {
                this.currentProject = SchematicProject.fromJson(el.getAsJsonObject());
                return this.currentProject != null;
            }
        }

        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.failed_to_load_project");

        return false;
    }

    public void closeCurrentProject()
    {
        if (this.currentProject != null)
        {
            this.currentProject.saveToFile();
            this.removeCurrentPlacement();
            this.currentProject = null;
        }
    }

    private void removeCurrentPlacement()
    {
        if (this.currentProject != null)
        {
            this.currentProject.removeCurrentPlacement();
        }
    }

    public void clear()
    {
        this.currentProject = null;
    }

    public boolean cycleVersion(int amount)
    {
        if (this.currentProject != null)
        {
            return this.currentProject.cycleVersion(amount);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.no_project_open");
        }

        return false;
    }

    public boolean commitNewVersion(String string)
    {
        if (this.currentProject != null)
        {
            return this.currentProject.commitNewVersion(string);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_versioning.no_project_open");
        }

        return false;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        if (this.currentProject != null)
        {
            obj.add("current_project", new JsonPrimitive(this.currentProject.getProjectFile().getAbsolutePath()));
        }

        return obj;
    }

    public void loadFromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "current_project"))
        {
            File file = new File(JsonUtils.getString(obj, "current_project"));
            JsonElement el = JsonUtils.parseJsonFile(file);

            if (el != null && el.isJsonObject())
            {
                this.currentProject = SchematicProject.fromJson(el.getAsJsonObject());
            }
        }
    }
}
