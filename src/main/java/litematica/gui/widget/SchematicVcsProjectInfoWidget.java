package litematica.gui.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import malilib.util.position.BlockPos;
import litematica.data.DataManager;
import litematica.schematic.projects.SchematicProject;
import litematica.schematic.projects.SchematicVersion;

public class SchematicVcsProjectInfoWidget extends ContainerWidget
{
    @Nullable protected SchematicProject currentProject;
    protected SimpleDateFormat dateFormat;

    public SchematicVcsProjectInfoWidget(int width, int height)
    {
        super(width, height);

        this.dateFormat = AbstractSchematicInfoWidget.createDateFormat();
        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        this.updateProjectInfo();
    }

    @Override
    public void updateSubWidgetPositions()
    {
        this.updateProjectInfo();
    }

    public void updateProjectInfo()
    {
        this.clearWidgets();

        if (this.currentProject == null)
        {
            return;
        }

        int x = this.getX() + 4;
        int y = this.getY() + 4;
        LabelWidget label = this.createInfoLabelWidget(x, y, this.currentProject);
        this.addWidget(label);
    }

    @Nullable
    public SchematicProject getSelectedSchematicProject()
    {
        return this.currentProject;
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        SchematicProject project;

        if (entry != null)
        {
            project = DataManager.getSchematicProjectsManager().loadProjectFromFile(entry.getFullPath(), false);
        }
        else
        {
            project = null;
        }

        this.setCurrentProject(project);
    }

    public void setCurrentProject(@Nullable SchematicProject project)
    {
        this.currentProject = project;
        this.updateProjectInfo();
    }

    protected LabelWidget createInfoLabelWidget(int x, int y, SchematicProject project)
    {
        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "litematica.label.schematic_vcs.info_widget.project", project.getName());

        int versionId = project.getCurrentVersionId();
        String currentVersion = (versionId >= 0 ? String.valueOf(versionId + 1) :
                                StringUtils.translate("litematica.label.misc.not_available"));
        StyledTextLine.translate(lines, "litematica.label.schematic_vcs.info_widget.current_version",
                                 currentVersion, project.getVersionCount());

        SchematicVersion version = project.getCurrentVersion();

        if (version != null)
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_vcs.info_widget.version_name",
                                     version.getName());

            StyledTextLine.translate(lines, "litematica.label.schematic_vcs.info_widget.version_time",
                                     this.dateFormat.format(new Date(version.getTimeStamp())));

            BlockPos o = project.getOrigin();
            StyledTextLine.translate(lines, "litematica.label.schematic_vcs.info_widget.origin",
                                     o.getX(), o.getY(), o.getZ());
        }

        LabelWidget label = new LabelWidget();
        label.setPosition(x, y);
        label.setLineHeight(12);
        label.setLines(lines);

        return label;
    }
}
