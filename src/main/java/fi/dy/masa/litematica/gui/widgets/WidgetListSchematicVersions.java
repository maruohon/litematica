package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.gui.GuiSchematicProjectManager;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.util.BaseGuiIcon;
import fi.dy.masa.malilib.gui.widget.WidgetListBase;
import fi.dy.masa.malilib.gui.widget.WidgetSearchBar;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.HorizontalAlignment;
import fi.dy.masa.malilib.util.StringUtils;

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
        this.setBackgroundColor(0xB0000000);
        this.setBorderColor(GuiBase.COLOR_HORIZONTAL_BAR);
        this.setBackgroundEnabled(true);

        this.addSearchBarWidget(new WidgetSearchBar(x + 2, y + 4, width - 14, 14, 0, BaseGuiIcon.SEARCH, HorizontalAlignment.LEFT));
    }

    @Override
    protected int getBackgroundWidth()
    {
        return this.browserWidth;
    }

    @Override
    protected int getBackgroundHeight()
    {
        return this.browserHeight;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, boolean hovered)
    {
        super.render(mouseX, mouseY, isActiveGui, hovered);
        this.drawAdditionalContents(mouseX, mouseY);
    }

    protected void drawAdditionalContents(int mouseX, int mouseY)
    {
        int x = this.getX() + this.getWidth() - this.infoWidth + 4;
        int y = this.getY() + 4;
        int infoHeight = 140;
        String str;
        String w = GuiBase.TXT_WHITE;
        String r = GuiBase.TXT_RST;
        int color = 0xFFB0B0B0;

        RenderUtils.drawOutlinedBox(x - 4, y - 4, this.infoWidth, infoHeight, 0xA0000000, GuiBase.COLOR_HORIZONTAL_BAR, this.getZLevel());

        str = StringUtils.translate("litematica.gui.label.schematic_projects.project");
        this.drawString(x, y, color, str);
        y += 12;
        this.drawString(x + 4, y, color, w + this.project.getName() + r);
        y += 12;
        int versionId = this.project.getCurrentVersionId();
        String strVer = w + (versionId >= 0 ? String.valueOf(versionId + 1) : "N/A") + r;
        str = StringUtils.translate("litematica.gui.label.schematic_projects.version", strVer, w + this.project.getVersionCount() + r);
        this.drawString(x, y, color, str);
        y += 12;
        SchematicVersion version = this.project.getCurrentVersion();

        if (version != null)
        {
            ToolHud.DATE.setTime(version.getTimeStamp());
            str = ToolHud.SIMPLE_DATE_FORMAT.format(ToolHud.DATE);
            str = StringUtils.translate("litematica.hud.schematic_projects.current_version_date", w + str + r);
            this.drawString(x, y, color, str);
            y += 12;

            str = StringUtils.translate("litematica.gui.label.schematic_projects.version_name");
            this.drawString(x, y, color, str);
            y += 12;
            this.drawString(x + 4, y, color, w + version.getName() + r);
            y += 12;
            str = StringUtils.translate("litematica.gui.label.schematic_projects.origin");
            this.drawString(x, y, color, str);
            y += 12;

            BlockPos o = this.project.getOrigin();
            str = String.format("x: %s%d%s, y: %s%d%s, z: %s%d%s", w, o.getX(), r, w, o.getY(), r, w, o.getZ(), r);
            this.drawString(x, y, color, str);
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
