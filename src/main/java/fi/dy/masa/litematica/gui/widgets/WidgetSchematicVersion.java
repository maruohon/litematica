package fi.dy.masa.litematica.gui.widgets;

import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseListEntryWidget;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicVersion extends BaseListEntryWidget<SchematicVersion>
{
    private final SchematicProject project;
    private final boolean isOdd;

    public WidgetSchematicVersion(int x, int y, int width, int height, boolean isOdd,
            SchematicVersion entry, int listIndex, SchematicProject project)
    {
        super(x, y, width, height, entry, listIndex);

        this.project = project;
        this.isOdd = isOdd;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId, boolean selected)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean versionSelected = this.project.getCurrentVersion() == this.entry;
        int x = this.getX();
        int y = this.getY();
        int z = this.getZLevel();
        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered and the selected entry
        if (selected || versionSelected || (isActiveGui && this.getId() == hoveredWidgetId))
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0707070, z);
        }
        else if (this.isOdd)
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0101010, z);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.renderRectangle(x, y, width, height, 0xA0303030, z);
        }

        if (versionSelected)
        {
            RenderUtils.renderOutline(x, y, width, height, 1, 0xFFE0E0E0, z);
        }

        String str = StringUtils.translate("litematica.gui.label.schematic_projects.version_entry", this.entry.getVersion(), this.entry.getName());
        this.drawString(x + 4, y + this.getCenteredTextOffsetY(), 0xFFFFFFFF, str);

        RenderUtils.color(1f, 1f, 1f, 1f);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        /*
        List<String> text = new ArrayList<>();

        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_name", this.placement.getSchematic().getMetadata().getName()));
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.hover.schematic_file", fileName));
        BlockPos o = this.placement.getOrigin();
        String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.origin", strOrigin));
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.sub_region_count", String.valueOf(this.placement.getSubRegionCount())));

        Vec3i size = this.placement.getSchematic().getTotalSize();
        String strSize = String.format("%d x %d x %d", size.getX(), size.getY(), size.getZ());
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.enclosing_size", strSize));

        RenderUtils.drawHoverText(mouseX, mouseY, this.getZLevel() + 1, text);
        */
    }
}
