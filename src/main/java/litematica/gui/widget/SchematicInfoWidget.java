package litematica.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.util.math.Vec3i;

import malilib.gui.icon.BaseIcon;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.render.text.StyledText;
import malilib.render.text.StyledTextLine;
import malilib.render.text.StyledTextUtils;
import malilib.util.StringUtils;
import litematica.gui.util.SchematicInfoCache;
import litematica.gui.util.SchematicInfoCache.SchematicInfo;
import litematica.schematic.SchematicMetadata;

public class SchematicInfoWidget extends ContainerWidget
{
    protected final SchematicInfoCache infoCache = new SchematicInfoCache();
    protected final LabelWidget infoTextLabel;
    protected final LabelWidget descriptionLabel;
    protected final IconWidget iconWidget;
    @Nullable protected SchematicInfo currentInfo;
    protected boolean hasDescription;

    public SchematicInfoWidget(int width, int height)
    {
        super(width, height);

        this.infoTextLabel = new LabelWidget();
        this.infoTextLabel.setLineHeight(12);

        this.descriptionLabel = new LabelWidget();
        this.descriptionLabel.setLineHeight(12);

        this.iconWidget = new IconWidget(null);
        this.iconWidget.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
        // FIXME (malilib) this seems silly to have to be enabled for the border to render (see IconWidget#renderAt())
        this.iconWidget.getBackgroundRenderer().getNormalSettings().setEnabled(true);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        if (this.currentInfo == null)
        {
            return;
        }

        this.addWidget(this.infoTextLabel);
        this.addWidgetIf(this.descriptionLabel, this.hasDescription);

        if (this.iconWidget.getIcon() != null)
        {
            this.addWidget(this.iconWidget);
        }
    }

    @Override
    public void updateSubWidgetPositions()
    {
        if (this.currentInfo == null)
        {
            return;
        }

        int x = this.getX() + 4;
        int y = this.getY() + 4;

        this.infoTextLabel.setPosition(x, y);

        if (this.hasDescription)
        {
            this.descriptionLabel.setPosition(x + 4, this.infoTextLabel.getBottom() + 2);
        }

        int offX = (this.getWidth() - this.iconWidget.getWidth()) / 2;
        this.iconWidget.setPosition(this.getX() + offX, this.getBottom() - this.iconWidget.getHeight() - 4);
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
            this.currentInfo = this.infoCache.getOrCacheSchematicInfo(entry.getFullPath());
        }
        else
        {
            this.currentInfo = null;
        }

        this.updateWidgetState();
        this.updateSubWidgetPositions();
        this.reAddSubWidgets();
    }

    @Override
    public void updateWidgetState()
    {
        // Note: the text needs to be updated first, to know the available space left for the icon
        this.updateInfoLabelText();
        this.updatePreviewIcon();
    }

    protected void updatePreviewIcon()
    {
        if (this.currentInfo == null || this.currentInfo.texture == null)
        {
            this.iconWidget.setIcon(null);
            return;
        }

        int iconSize = (int) Math.sqrt(this.currentInfo.texture.getTextureData().length);
        int usableHeight = this.getHeight() - this.infoTextLabel.getHeight() - 4;

        if (this.hasDescription)
        {
            usableHeight -= this.descriptionLabel.getHeight() + 4;
        }

        if (usableHeight < iconSize)
        {
            iconSize = usableHeight;
        }

        // No point showing so small previews that you can't see anything from it
        if (iconSize < 10)
        {
            this.iconWidget.setIcon(null);
        }
        else
        {
            this.iconWidget.setIcon(new BaseIcon(0, 0, iconSize, iconSize, iconSize, iconSize, this.currentInfo.iconName));
        }
    }

    protected void updateInfoLabelText()
    {
        if (this.currentInfo == null)
        {
            this.infoTextLabel.setLabelStyledTextLines(Collections.emptyList());
            this.descriptionLabel.setLabelStyledTextLines(Collections.emptyList());
            this.descriptionLabel.getHoverInfoFactory().setTextLines("desc", Collections.emptyList());
            return;
        }

        SchematicMetadata meta = this.currentInfo.schematic.getMetadata();
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

        this.hasDescription = org.apache.commons.lang3.StringUtils.isBlank(meta.getDescription()) == false;

        if (this.hasDescription)
        {
            int usableHeight = this.getBottom() - this.getY() - 12;
            int maxLines = usableHeight / 12;
            int maxDescLines = maxLines - lines.size();

            StyledTextLine.translate(lines, "litematica.label.schematic_info.description");
            StyledText nonWrappedText = StyledText.translate("litematica.label.schematic_info.generic_value", meta.getDescription());
            boolean fitAll = false;

            if (maxDescLines > 1)
            {
                StyledText wrappedText = StyledTextUtils.wrapStyledTextToMaxWidth(nonWrappedText, this.getWidth() - 8);
                List<StyledTextLine> wrappedLines = wrappedText.getLines();
                List<StyledTextLine> descriptionLines;

                if (wrappedLines.size() <= maxDescLines)
                {
                    descriptionLines = wrappedLines;
                    fitAll = true;
                }
                else
                {
                    descriptionLines = new ArrayList<>();

                    if (maxDescLines >= 2)
                    {
                        descriptionLines.addAll(wrappedLines.subList(0, maxDescLines - 1));
                    }

                    int more = wrappedLines.size() - maxDescLines - 1;
                    descriptionLines.add(StyledTextLine.translate("litematica.label.schematic_info.description.more", more));
                }

                this.descriptionLabel.setLabelStyledTextLines(descriptionLines);
            }

            if (fitAll == false)
            {
                this.descriptionLabel.getHoverInfoFactory().setTextLines("desc", nonWrappedText.getLines());
            }
        }

        this.infoTextLabel.setLabelStyledTextLines(lines);
    }
}
