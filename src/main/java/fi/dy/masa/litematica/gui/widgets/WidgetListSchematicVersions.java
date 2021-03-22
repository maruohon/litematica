package fi.dy.masa.litematica.gui.widgets;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.gui.GuiSchematicProjectManager;
import fi.dy.masa.litematica.render.infohud.ToolHud;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.SearchBarWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseListWidget;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.gui.position.HorizontalAlignment;

public class WidgetListSchematicVersions extends BaseListWidget<SchematicVersion, WidgetSchematicVersion>
{
    private final SchematicProject project;
    protected final int infoWidth;

    public WidgetListSchematicVersions(int x, int y, int width, int height, SchematicProject project, GuiSchematicProjectManager parent)
    {
        super(x, y, width, height, parent);

        this.project = project;
        this.entryWidgetFixedHeight = 16;
        this.infoWidth = 180;
        this.setBackgroundColor(0xB0000000);
        this.setNormalBorderColor(BaseScreen.COLOR_HORIZONTAL_BAR);
        this.setBackgroundEnabled(true);

        this.addSearchBarWidget(new SearchBarWidget(x + 2, y + 4, width - 14, 14, 0, DefaultIcons.SEARCH, HorizontalAlignment.LEFT));
    }

    @Override
    protected int getBackgroundWidth()
    {
        return this.listWidth;
    }

    @Override
    protected int getBackgroundHeight()
    {
        return this.listHeight;
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
        String w = BaseScreen.TXT_WHITE;
        String r = BaseScreen.TXT_RST;
        int color = 0xFFB0B0B0;

        ShapeRenderUtils.renderOutlinedRectangle(x - 4, y - 4, this.getZLevel(), this.infoWidth, infoHeight, 0xA0000000, BaseScreen.COLOR_HORIZONTAL_BAR);

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
        return new WidgetSchematicVersion(x, y, this.entryWidgetWidth, this.getBrowserEntryHeightFor(entry),
                                          isOdd, entry, listIndex, this.project);
    }
}
