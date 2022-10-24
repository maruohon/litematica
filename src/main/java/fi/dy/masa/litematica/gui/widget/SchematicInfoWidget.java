package fi.dy.masa.litematica.gui.widget;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.util.math.Vec3i;

import malilib.gui.icon.BaseIcon;
import malilib.gui.icon.Icon;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.render.text.StyledText;
import malilib.render.text.StyledTextLine;
import malilib.render.text.StyledTextUtils;
import malilib.util.StringUtils;
import fi.dy.masa.litematica.gui.util.SchematicInfoCache;
import fi.dy.masa.litematica.gui.util.SchematicInfoCache.SchematicInfo;
import fi.dy.masa.litematica.schematic.SchematicMetadata;

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

    protected void reCreateSubWidgets()
    {
        this.clearWidgets();

        if (this.currentInfo == null)
        {
            return;
        }

        int x = this.getX() + 4;
        int y = this.getY() + 4;
        SchematicMetadata meta = this.currentInfo.schematic.getMetadata();
        LabelWidget label = this.createInfoLabelWidget(x, y, meta);
        this.addWidget(label);

        if (this.currentInfo.texture != null)
        {
            IconWidget iconWidget = this.createPreviewIconWidget(x, label.getBottom() + 4);
            this.addWidgetIfNotNull(iconWidget);
        }
    }

    @Nullable
    public SchematicInfo getSelectedSchematicInfo()
    {
        return this.currentInfo;
    }

    public void clearCache()
    {
        this.infoCache.clearCache();
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null)
        {
            Path file = entry.getFullPath();
            this.infoCache.cacheSchematicInfo(file);
            this.currentInfo = this.infoCache.getSchematicInfo(file);
        }
        else
        {
            this.currentInfo = null;
        }

        this.reCreateSubWidgets();
    }

    @Nullable
    protected IconWidget createPreviewIconWidget(int x, int y)
    {
        int iconSize = (int) Math.sqrt(this.currentInfo.texture.getTextureData().length);
        int textureSize = iconSize;
        int usableHeight = this.getBottom() - y - 4;

        if (usableHeight < iconSize)
        {
            iconSize = usableHeight;
        }

        // No point showing so small previews that you can't see anything from it
        if (iconSize < 10)
        {
            return null;
        }

        Icon icon = new BaseIcon(0, 0, iconSize, iconSize, textureSize, textureSize, this.currentInfo.iconName);
        IconWidget iconWidget = new IconWidget(icon);
        iconWidget.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
        iconWidget.setPosition(x, y);

        return iconWidget;
    }

    protected LabelWidget createInfoLabelWidget(int x, int y, SchematicMetadata meta)
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
            StyledText text = StyledText.translate("litematica.label.schematic_info.generic_value", meta.getDescription());
            text = StyledTextUtils.wrapStyledTextToMaxWidth(text, this.getWidth() - 6);
            lines.addAll(text.getLines());
        }

        LabelWidget label = new LabelWidget();
        label.setPosition(x, y);
        label.setLineHeight(12);
        label.setLabelStyledTextLines(lines);

        return label;
    }
}
