package de.meinbuild.liteschem.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import de.meinbuild.liteschem.gui.Icons;
import de.meinbuild.liteschem.render.infohud.ToolHud;
import de.meinbuild.liteschem.schematic.projects.SchematicProject;
import de.meinbuild.liteschem.schematic.projects.SchematicVersion;
import de.meinbuild.liteschem.gui.GuiSchematicProjectManager;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.LeftRight;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetSearchBar;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;

public class WidgetListSchematicVersions extends WidgetListBase<SchematicVersion, WidgetSchematicVersion>
{
    private final SchematicProject project;
    protected final int infoWidth;

    public WidgetListSchematicVersions(int x, int y, int width, int height, SchematicProject project, GuiSchematicProjectManager parent)
    {
        super(x, y, width, height, parent);

        this.project = project;
        this.browserEntryHeight = 16;
        this.infoWidth = 180;
        this.widgetSearchBar = new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, Icons.FILE_ICON_SEARCH, LeftRight.LEFT);
        this.browserEntriesOffsetY = this.widgetSearchBar.getHeight() + 3;
    }

    @Override
    public void drawContents(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        // Draw an outline around the entire entry list
        RenderUtils.drawOutlinedBox(this.posX, this.posY, this.browserWidth, this.browserHeight, 0xB0000000, COLOR_HORIZONTAL_BAR);

        super.drawContents(matrixStack, mouseX, mouseY, partialTicks);

        this.drawAdditionalContents(mouseX, mouseY, matrixStack);
    }

    protected void drawAdditionalContents(int mouseX, int mouseY, MatrixStack matrixStack)
    {
        int x = this.posX + this.totalWidth - this.infoWidth + 4;
        int y = this.posY + 4;
        int infoHeight = 140;
        String str;
        String w = GuiBase.TXT_WHITE;
        String r = GuiBase.TXT_RST;
        int color = 0xFFB0B0B0;

        RenderUtils.drawOutlinedBox(x - 4, y - 4, this.infoWidth, infoHeight, 0xA0000000, COLOR_HORIZONTAL_BAR);

        str = StringUtils.translate("litematica.gui.label.schematic_projects.project");
        this.drawString(matrixStack, str, x, y, color);
        y += 12;
        this.drawString(matrixStack, w + this.project.getName() + r, x + 4, y, color);
        y += 12;
        int versionId = this.project.getCurrentVersionId();
        String strVer = w + (versionId >= 0 ? String.valueOf(versionId + 1) : "N/A") + r;
        str = StringUtils.translate("litematica.gui.label.schematic_projects.version", strVer, w + this.project.getVersionCount() + r);
        this.drawString(matrixStack, str, x, y, color);
        y += 12;
        SchematicVersion version = this.project.getCurrentVersion();

        if (version != null)
        {
            ToolHud.DATE.setTime(version.getTimeStamp());
            str = ToolHud.SIMPLE_DATE_FORMAT.format(ToolHud.DATE);
            str = StringUtils.translate("litematica.hud.schematic_projects.current_version_date", w + str + r);
            this.drawString(matrixStack, str, x, y, color);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.schematic_projects.version_name");
            this.drawString(matrixStack, str, x, y, color);
            y += 12;
            this.drawString(matrixStack, w + version.getName() + r, x + 4, y, color);
            y += 12;
            str = StringUtils.translate("litematica.gui.label.schematic_projects.origin");
            this.drawString(matrixStack, str, x, y, color);
            y += 12;

            BlockPos o = this.project.getOrigin();
            str = String.format("x: %s%d%s, y: %s%d%s, z: %s%d%s", w, o.getX(), r, w, o.getY(), r, w, o.getZ(), r);
            this.drawString(matrixStack, str, x, y, color);
        }
    }

    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);

        this.browserWidth = width - this.infoWidth - 6;
        this.browserEntryWidth = this.browserWidth - 14;
    }

    @Override
    protected Collection<SchematicVersion> getAllEntries()
    {
        return this.project.getAllVersions();
    }

    @Override
    protected List<String> getEntryStringsForFilter(SchematicVersion entry)
    {
        return ImmutableList.of(entry.getName().toLowerCase(), entry.getFileName().toLowerCase());
    }

    @Override
    protected WidgetSchematicVersion createListEntryWidget(int x, int y, int listIndex, boolean isOdd, SchematicVersion entry)
    {
        return new WidgetSchematicVersion(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, entry, listIndex, this.project);
    }
}
