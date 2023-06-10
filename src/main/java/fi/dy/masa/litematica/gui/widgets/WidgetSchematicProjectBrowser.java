package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import javax.annotation.Nullable;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicProjectBrowser extends WidgetFileBrowserBase implements ISelectionListener<DirectoryEntry>
{
    @Nullable private SchematicProject selectedProject;
    private final ISelectionListener<DirectoryEntry> selectionListener;
    protected final int infoWidth;

    public WidgetSchematicProjectBrowser(int x, int y, int width, int height, ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), "version_control",
                DataManager.getSchematicsBaseDirectory(), null, Icons.DUMMY);

        this.selectionListener = selectionListener;
        this.browserEntryHeight = 14;
        this.infoWidth = 170;
        this.setSelectionListener(this);
    }

    @Override
    protected File getRootDirectory()
    {
        return DataManager.getSchematicsBaseDirectory();
    }

    @Override
    protected FileFilter getFileFilter()
    {
        return WidgetAreaSelectionBrowser.JSON_FILTER;
    }

    @Override
    protected int getBrowserWidthForTotalWidth(int width)
    {
        return super.getBrowserWidthForTotalWidth(width) - this.infoWidth;
    }

    @Override
    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null)
        {
            if (entry.getType() == DirectoryEntryType.FILE)
            {
                this.selectedProject = DataManager.getSchematicProjectsManager().loadProjectFromFile(entry.getFullPath(), false);
            }
            else
            {
                this.selectedProject = null;
            }
        }

        this.selectionListener.onSelectionChange(entry);
    }

    @Override
    protected void drawAdditionalContents(int mouseX, int mouseY, DrawContext drawContext)
    {
        int x = this.posX + this.totalWidth - this.infoWidth + 4;
        int y = this.posY + 4;
        int infoHeight = 100;
        RenderUtils.drawOutlinedBox(x - 4, y - 4, this.infoWidth, infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

        SchematicProject project = this.selectedProject;

        if (project != null)
        {
            String str;
            String w = GuiBase.TXT_WHITE;
            String r = GuiBase.TXT_RST;
            int color = 0xFFB0B0B0;

            str = StringUtils.translate("litematica.gui.label.schematic_projects.project");
            this.drawString(drawContext, str, x, y, color);
            y += 12;
            this.drawString(drawContext, w + project.getName() + r, x + 8, y, color);
            y += 12;
            int versionId = project .getCurrentVersionId();
            String strVer = w + (versionId >= 0 ? String.valueOf(versionId + 1) : "N/A") + r;
            str = StringUtils.translate("litematica.gui.label.schematic_projects.version", strVer, w + project.getVersionCount() + r);
            this.drawString(drawContext, str, x, y, color);
            y += 12;
            SchematicVersion version = project.getCurrentVersion();

            if (version != null)
            {
                str = StringUtils.translate("litematica.gui.label.schematic_projects.origin");
                this.drawString(drawContext, str, x, y, color);
                y += 12;

                BlockPos o = project.getOrigin();
                str = String.format("x: %s%d%s, y: %s%d%s, z: %s%d%s", w, o.getX(), r, w, o.getY(), r, w, o.getZ(), r);
                this.drawString(drawContext, str, x + 8, y, color);
            }
        }
    }
}
