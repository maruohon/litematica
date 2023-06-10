package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;

import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSchematicVersion extends WidgetListEntryBase<SchematicVersion>
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
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean versionSelected = this.project.getCurrentVersion() == this.entry;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || versionSelected || this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, 0xA0303030);
        }

        if (versionSelected)
        {
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        String str = StringUtils.translate("litematica.gui.label.schematic_projects.version_entry", this.entry.getVersion(), this.entry.getName());
        this.drawString(this.x + 4, this.y + 4, 0xFFFFFFFF, str, drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext)
    {
        List<String> text = new ArrayList<>();
        /*
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_name", this.placement.getSchematic().getMetadata().getName()));
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.schematic_file", fileName));
        BlockPos o = this.placement.getOrigin();
        String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.origin", strOrigin));
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.sub_region_count", String.valueOf(this.placement.getSubRegionCount())));

        Vec3i size = this.placement.getSchematic().getTotalSize();
        String strSize = String.format("%d x %d x %d", size.getX(), size.getY(), size.getZ());
        text.add(StringUtils.translate("litematica.gui.label.schematic_placement.enclosing_size", strSize));
        */

        RenderUtils.drawHoverText(mouseX, mouseY, text, drawContext);
    }
}
