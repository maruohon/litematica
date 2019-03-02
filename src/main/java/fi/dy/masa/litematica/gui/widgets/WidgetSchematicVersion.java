package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.schematic.projects.SchematicProject;
import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;

public class WidgetSchematicVersion extends WidgetListEntryBase<SchematicVersion>
{
    private final SchematicProject project;
    private final boolean isOdd;

    public WidgetSchematicVersion(int x, int y, int width, int height, float zLevel, boolean isOdd,
            SchematicVersion entry, int listIndex, SchematicProject project)
    {
        super(x, y, width, height, zLevel, entry, listIndex);

        this.project = project;
        this.isOdd = isOdd;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        GlStateManager.color(1, 1, 1, 1);

        boolean versionSelected = this.project.getCurrentVersion() == this.entry;

        // Draw a lighter background for the hovered and the selected entry
        if (selected || versionSelected || this.isMouseOver(mouseX, mouseY))
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0303030);
        }

        if (versionSelected)
        {
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0);
        }

        String str = I18n.format("litematica.gui.label.schematic_projects.version_entry", this.entry.getVersion(), this.entry.getName());
        this.mc.fontRenderer.drawString(str, this.x + 4, this.y + 4, 0xFFFFFFFF);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        List<String> text = new ArrayList<>();
        /*
        text.add(I18n.format("litematica.gui.label.schematic_placement.schematic_name", this.placement.getSchematic().getMetadata().getName()));
        text.add(I18n.format("litematica.gui.label.schematic_placement.schematic_file", fileName));
        BlockPos o = this.placement.getOrigin();
        String strOrigin = String.format("x: %d, y: %d, z: %d", o.getX(), o.getY(), o.getZ());
        text.add(I18n.format("litematica.gui.label.schematic_placement.origin", strOrigin));
        text.add(I18n.format("litematica.gui.label.schematic_placement.sub_region_count", String.valueOf(this.placement.getSubRegionCount())));

        Vec3i size = this.placement.getSchematic().getTotalSize();
        String strSize = String.format("%d x %d x %d", size.getX(), size.getY(), size.getZ());
        text.add(I18n.format("litematica.gui.label.schematic_placement.enclosing_size", strSize));
        */

        RenderUtils.drawHoverText(mouseX, mouseY, text);
    }
}
