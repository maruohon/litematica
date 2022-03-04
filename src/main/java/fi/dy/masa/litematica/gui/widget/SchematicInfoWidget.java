package fi.dy.masa.litematica.gui.widget;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.gui.util.SchematicInfoCache;
import fi.dy.masa.litematica.gui.util.SchematicInfoCache.SchematicInfo;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.malilib.gui.icon.BaseIcon;
import fi.dy.masa.malilib.gui.icon.Icon;
import fi.dy.masa.malilib.gui.widget.ContainerWidget;
import fi.dy.masa.malilib.gui.widget.IconWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.malilib.util.StringUtils;

public class SchematicInfoWidget extends ContainerWidget
{
    protected final SchematicInfoCache infoCache = new SchematicInfoCache();
    @Nullable protected SchematicInfo currentInfo;

    public SchematicInfoWidget(int width, int height)
    {
        super(width, height);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        this.reCreateSubWidgets();
    }

    @Override
    public void updateSubWidgetPositions()
    {
        this.reCreateSubWidgets();
    }

    public void onScreenClosed()
    {
        this.infoCache.deleteTextures();
    }

    protected void reCreateSubWidgets()
    {
        this.clearWidgets();

        if (this.currentInfo == null)
        {
            return;
        }

        SchematicMetadata meta = this.currentInfo.schematic.getMetadata();

        LabelWidget label = this.createInfoLabelWidget(meta);
        int x = this.getX() + 4;
        int y = this.getY() + 4;
        label.setPosition(x, y);
        label.setLineHeight(12);
        this.addWidget(label);

        if (this.currentInfo.texture != null)
        {
            y = label.getBottom() + 4;
            int iconSize = (int) Math.sqrt(this.currentInfo.texture.getTextureData().length);
            int textureSize = iconSize;
            int usableHeight = this.getBottom() - y - 4;

            if (usableHeight < iconSize)
            {
                iconSize = usableHeight;
            }

            Icon icon = new BaseIcon(0, 0, iconSize, iconSize, textureSize, textureSize, this.currentInfo.iconName);
            IconWidget iconWidget = new IconWidget(icon);
            iconWidget.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
            iconWidget.setPosition(x, y);

            this.addWidget(iconWidget);
        }
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null)
        {
            this.infoCache.cacheSchematicInfo(entry);
            this.currentInfo = this.infoCache.getSchematicInfo(entry.getFullPath());
        }
        else
        {
            this.currentInfo = null;
        }

        this.reCreateSubWidgets();
    }

    protected LabelWidget createInfoLabelWidget(SchematicMetadata meta)
    {
        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "litematica.label.schematic_info.name");
        StyledTextLine.translate(lines, "litematica.label.schematic_info.name.value", meta.getName());

        StyledTextLine.translate(lines, "litematica.label.schematic_info.schematic_author", meta.getAuthor());

        StyledTextLine.translate(lines, "litematica.label.schematic_info.time_created",
                                 BaseFileBrowserWidget.DATE_FORMAT.format(new Date(meta.getTimeCreated())));

        if (meta.hasBeenModified())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.time_modified",
                                     BaseFileBrowserWidget.DATE_FORMAT.format(new Date(meta.getTimeModified())));
        }

        StyledTextLine.translate(lines, "litematica.label.schematic_info.region_count", meta.getRegionCount());

        Vec3i areaSize = meta.getEnclosingSize();
        String areaSizeStr = StringUtils.translate("litematica.label.schematic_info.dimensions_value",
                                                   areaSize.getX(), areaSize.getY(), areaSize.getZ());

        if (this.getHeight() >= 240)
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.total_volume", meta.getTotalVolume());
            StyledTextLine.translate(lines, "litematica.label.schematic_info.total_blocks", meta.getTotalBlocks());
            StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size");
            StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size.value", areaSizeStr);
        }
        else
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.total_blocks_and_volume",
                                     meta.getTotalBlocks(), meta.getTotalVolume());
            StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size_and_value", areaSizeStr);
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(meta.getDescription()) == false)
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.description");
            StyledTextLine.translate(lines, "litematica.label.schematic_info.generic_value", meta.getDescription());
        }

        LabelWidget label = new LabelWidget();
        label.setLabelStyledTextLines(lines);

        return label;
    }
}
