package fi.dy.masa.litematica.schematic.projects;

import java.io.File;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.MinecraftClient;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.gui.GuiSchematicProjectManager;
import fi.dy.masa.litematica.gui.GuiSchematicProjectsBrowser;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;

public class SchematicProjectsManager
{
    //private static final Pattern PATTERN_NAME_NUMBER = Pattern.compile("(.*)([0-9]+)$");
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Nullable
    private SchematicProject currentProject;

    public void openSchematicProjectsGui()
    {
        if (Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.getBooleanValue() == false)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 10000, "litematica.message.warning.schematic_projects_hidden");
            return;
        }

        if (this.currentProject != null)
        {
            GuiSchematicProjectManager gui = new GuiSchematicProjectManager(this.currentProject);
            gui.setParent(GuiUtils.getCurrentScreen());
            GuiBase.openGui(gui);
        }
        else
        {
            GuiSchematicProjectsBrowser gui = new GuiSchematicProjectsBrowser();
            gui.setParent(GuiUtils.getCurrentScreen());
            GuiBase.openGui(gui);
        }
    }

    @Nullable
    public SchematicProject getCurrentProject()
    {
        return this.hasProjectOpen() ? this.currentProject : null;
    }

    public boolean hasProjectOpen()
    {
        return this.currentProject != null && Configs.Generic.UNHIDE_SCHEMATIC_PROJECTS.getBooleanValue();
    }

    public void createNewProject(File dir, String projectName)
    {
        this.closeCurrentProject();

        this.currentProject = new SchematicProject(dir, new File(dir, projectName + ".json"));
        this.currentProject.setName(projectName);
        this.currentProject.setOrigin(fi.dy.masa.malilib.util.PositionUtils.getEntityBlockPos(this.mc.player));
        this.currentProject.saveToFile();
    }

    public boolean openProject(File projectFile)
    {
        this.closeCurrentProject();

        this.currentProject = this.loadProjectFromFile(projectFile, true);

        if (this.currentProject == null)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.failed_to_load_project");
            return false;
        }

        return true;
    }

    @Nullable
    public SchematicProject loadProjectFromFile(File projectFile, boolean createPlacement)
    {
        if (projectFile.getName().endsWith(".json") && projectFile.exists() && projectFile.isFile() && projectFile.canRead())
        {
            JsonElement el = JsonUtils.parseJsonFile(projectFile);

            if (el != null && el.isJsonObject())
            {
                return SchematicProject.fromJson(el.getAsJsonObject(), projectFile, createPlacement);
            }
        }

        return null;
    }

    public void closeCurrentProject()
    {
        if (this.currentProject != null)
        {
            this.currentProject.saveToFile();
            this.removeCurrentPlacement();
            this.clear();
        }
    }

    public void saveCurrentProject()
    {
        if (this.currentProject != null)
        {
            this.currentProject.saveToFile();
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
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.no_project_open");
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
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_projects.no_project_open");
        }

        return false;
    }

    public boolean pasteCurrentVersionToWorld()
    {
        SchematicProject project = this.getCurrentProject();

        if (project != null)
        {
            project.pasteToWorld();
            return true;
        }

        return false;
    }

    public boolean deleteLastSeenArea(MinecraftClient mc)
    {
        SchematicProject project = this.getCurrentProject();

        if (project != null)
        {
            project.deleteLastSeenArea(mc);
            return true;
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
            this.currentProject = this.loadProjectFromFile(file, true);
        }
    }
}
