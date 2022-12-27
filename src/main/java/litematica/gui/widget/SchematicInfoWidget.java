package litematica.gui.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.util.math.Vec3i;

import malilib.gui.BaseScreen;
import malilib.gui.icon.BaseIcon;
import malilib.gui.icon.DefaultIcons;
import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.IconWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.BaseFileBrowserWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.render.text.StyledText;
import malilib.render.text.StyledTextLine;
import malilib.render.text.StyledTextUtils;
import malilib.util.StringUtils;
import litematica.config.Configs;
import litematica.gui.SchematicInfoConfigScreen;
import litematica.gui.util.SchematicInfoCache;
import litematica.gui.util.SchematicInfoCache.SchematicInfo;
import litematica.schematic.SchematicMetadata;

public class SchematicInfoWidget extends ContainerWidget
{
    protected final SchematicInfoCache infoCache = new SchematicInfoCache();
    protected final LabelWidget infoTextLabel;
    protected final LabelWidget descriptionLabel;
    protected final IconWidget iconWidget;
    protected final GenericButton configButton;
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

        this.configButton = GenericButton.create(DefaultIcons.INFO_ICON_11, this::openConfigScreen);

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

        this.addWidget(this.configButton);
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
        this.configButton.setPosition(this.getRight() - 14, this.getY() + 3);
    }

    protected void openConfigScreen()
    {
        BaseScreen.openPopupScreenWithCurrentScreenAsParent(new SchematicInfoConfigScreen());
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
        if (this.currentInfo == null || this.currentInfo.texture == null ||
            Configs.Internal.SCHEMATIC_INFO_SHOW_THUMBNAIL.getBooleanValue() == false)
        {
            this.iconWidget.setIcon(null);
            return;
        }

        int iconSize = (int) Math.sqrt(this.currentInfo.texture.getTextureData().length);
        int usableHeight = this.getHeight() - this.infoTextLabel.getHeight() - 12;

        if (this.hasDescription)
        {
            usableHeight -= this.descriptionLabel.getHeight() + 6;
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

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_NAME.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.name");
            StyledTextLine.translate(lines, "litematica.label.schematic_info.name.value", meta.getName());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_AUTHOR.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.schematic_author", meta.getAuthor());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_CREATION_TIME.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.time_created",
                                     BaseFileBrowserWidget.DATE_FORMAT.format(new Date(meta.getTimeCreated())));
        }

        if (meta.hasBeenModified() && Configs.Internal.SCHEMATIC_INFO_SHOW_MODIFICATION_TIME.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.time_modified",
                                     BaseFileBrowserWidget.DATE_FORMAT.format(new Date(meta.getTimeModified())));
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_MC_VERSION.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.mc_version",
                                     meta.getMinecraftVersion(), meta.getMinecraftDataVersion());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_SCHEMATIC_VERSION.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.schematic_version", meta.getSchematicVersion());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_REGION_COUNT.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.region_count", meta.getRegionCount());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_ENTITY_COUNT.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.entity_count", meta.getEntityCount());
        }

        if (Configs.Internal.SCHEMATIC_INFO_SHOW_BLOCKENTITY_COUNT.getBooleanValue())
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.block_entity_count", meta.getBlockEntityCount());
        }

        Vec3i areaSize = meta.getEnclosingSize();
        String areaSizeStr = StringUtils.translate("litematica.label.schematic_info.dimensions_value",
                                                   areaSize.getX(), areaSize.getY(), areaSize.getZ());

        if (this.getHeight() >= 240)
        {
            if (Configs.Internal.SCHEMATIC_INFO_SHOW_TOTAL_BLOCKS.getBooleanValue())
            {
                StyledTextLine.translate(lines, "litematica.label.schematic_info.total_blocks", meta.getTotalBlocks());
            }

            if (Configs.Internal.SCHEMATIC_INFO_SHOW_TOTAL_VOLUME.getBooleanValue())
            {
                StyledTextLine.translate(lines, "litematica.label.schematic_info.total_volume", meta.getTotalVolume());
            }

            if (Configs.Internal.SCHEMATIC_INFO_SHOW_ENCLOSING_SIZE.getBooleanValue())
            {
                StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size");
                StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size.value", areaSizeStr);
            }
        }
        else
        {
            if (Configs.Internal.SCHEMATIC_INFO_SHOW_TOTAL_VOLUME.getBooleanValue() ||
                Configs.Internal.SCHEMATIC_INFO_SHOW_TOTAL_BLOCKS.getBooleanValue())
            {
                StyledTextLine.translate(lines, "litematica.label.schematic_info.total_blocks_and_volume",
                                         meta.getTotalBlocks(), meta.getTotalVolume());
                StyledTextLine.translate(lines, "litematica.label.schematic_info.enclosing_size_and_value", areaSizeStr);
            }
        }

        this.hasDescription = org.apache.commons.lang3.StringUtils.isBlank(meta.getDescription()) == false;

        if (this.hasDescription)
        {
            StyledTextLine.translate(lines, "litematica.label.schematic_info.description");

            int usableHeight = this.getHeight() - 12;
            int maxLines = usableHeight / 12;
            int maxDescLines = maxLines - lines.size();

            StyledText nonWrappedText = StyledText.translate("litematica.label.schematic_info.generic_value", meta.getDescription());
            boolean fitAll = false;
            List<StyledTextLine> descriptionLines;

            if (maxDescLines > 0 && Configs.Internal.SCHEMATIC_INFO_SHOW_DESCRIPTION.getBooleanValue())
            {
                StyledText wrappedText = StyledTextUtils.wrapStyledTextToMaxWidth(nonWrappedText, this.getWidth() - 8);
                List<StyledTextLine> wrappedLines = wrappedText.getLines();

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
                    StyledTextLine.translate(descriptionLines, "litematica.label.schematic_info.description.more", more);
                }

            }
            // Has a description, but showing it is not enabled, add a line that can be hovered
            else
            {
                descriptionLines = new ArrayList<>();
                StyledTextLine.translate(descriptionLines, "litematica.label.schematic_info.description.hover");
            }

            this.descriptionLabel.setLabelStyledTextLines(descriptionLines);

            if (fitAll == false)
            {
                this.descriptionLabel.getHoverInfoFactory().setTextLines("desc", nonWrappedText.getLines());
            }
        }

        this.infoTextLabel.setLabelStyledTextLines(lines);
    }
}
