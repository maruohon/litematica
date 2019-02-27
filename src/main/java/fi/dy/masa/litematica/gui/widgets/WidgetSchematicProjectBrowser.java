package fi.dy.masa.litematica.gui.widgets;

import java.io.File;
import java.io.FileFilter;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.Icons;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.schematic.versioning.SchematicProject;
import fi.dy.masa.litematica.schematic.versioning.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;

public class WidgetSchematicProjectBrowser extends WidgetFileBrowserBase
{
    protected final int infoWidth;

    public WidgetSchematicProjectBrowser(int x, int y, int width, int height, ISelectionListener<DirectoryEntry> selectionListener)
    {
        super(x, y, width, height, DataManager.getDirectoryCache(), "version_control",
                DataManager.getSchematicsBaseDirectory(), selectionListener, Icons.DUMMY);

        this.browserEntryHeight = 14;
        this.infoWidth = 170;
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
    protected void drawAdditionalContents(int mouseX, int mouseY)
    {
        SchematicProject project = DataManager.getSchematicVersionManager().getCurrentProject();

        if (project != null)
        {
            int x = this.posX + this.totalWidth - this.infoWidth + 4;
            int y = this.posY + 4;
            int infoHeight = 100;
            String str;
            String w = GuiBase.TXT_WHITE;
            String r = GuiBase.TXT_RST;
            int color = 0xFFB0B0B0;

            RenderUtils.drawOutlinedBox(x - 4, y - 4, this.infoWidth, infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

            str = I18n.format("litematica.gui.label.schematic_versioning.project");
            this.fontRenderer.drawString(str, x, y, color);
            y += 12;
            this.fontRenderer.drawString(w + project.getName() + r, x + 4, y, color);
            y += 12;
            int versionId = project .getCurrentVersionId();
            String strVer = w + (versionId >= 0 ? String.valueOf(versionId + 1) : "N/A") + r;
            str = I18n.format("litematica.gui.label.schematic_versioning.version", strVer, w + project.getVersionCount() + r);
            this.fontRenderer.drawString(str, x, y, color);
            y += 12;
            SchematicVersion version = project.getCurrentVersion();

            if (version != null)
            {
                ToolHud.DATE.setTime(version.getTimeStamp());
                str = ToolHud.SIMPLE_DATE_FORMAT.format(ToolHud.DATE);
                str = I18n.format("litematica.hud.version_control.current_version_date", w + str + r);
                this.fontRenderer.drawString(str, x, y, color);
                y += 12;

                str = I18n.format("litematica.gui.label.schematic_versioning.version_name");
                this.fontRenderer.drawString(str, x, y, color);
                y += 12;
                this.fontRenderer.drawString(w + version.getName() + r, x + 4, y, color);
                y += 12;
                BlockPos o = project.getOrigin();
                str = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
                str = I18n.format("litematica.gui.label.area_selection_origin", w + str + r);
                this.fontRenderer.drawString(str, x, y, color);
            }
        }
    }
}
