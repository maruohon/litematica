package fi.dy.masa.litematica.gui.widget;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.litematica.gui.util.SchematicPlacementInfoCache;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementUnloaded;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.widget.ContainerWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget;
import fi.dy.masa.malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import fi.dy.masa.malilib.overlay.message.MessageHelpers;
import fi.dy.masa.malilib.render.text.StyledTextLine;

public class SavedSchematicPlacementInfoWidget extends ContainerWidget
{
    protected final SchematicPlacementInfoCache infoCache = new SchematicPlacementInfoCache();
    @Nullable protected SchematicPlacementUnloaded currentInfo;

    public SavedSchematicPlacementInfoWidget(int width, int height)
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
        LabelWidget label = this.createInfoLabelWidget(x, y, this.currentInfo);
        this.addWidget(label);
    }

    @Nullable
    public SchematicPlacementUnloaded getSelectedPlacementInfo()
    {
        return this.currentInfo;
    }

    public void onSelectionChange(@Nullable DirectoryEntry entry)
    {
        if (entry != null)
        {
            this.currentInfo = this.infoCache.cacheAndGetPlacementInfo(entry.getFullPath());
        }
        else
        {
            this.currentInfo = null;
        }

        this.reCreateSubWidgets();
    }

    protected LabelWidget createInfoLabelWidget(int x, int y, SchematicPlacementUnloaded data)
    {
        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.region_count",
                                 data.getRegionCount());

        String stateStr = MessageHelpers.getOnOffColored(data.isEnabled(), true);
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.placement_state", stateStr);

        stateStr = MessageHelpers.getYesNoColored(data.isRegionPlacementModified(), false);
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.modified", stateStr);

        String rotationStr = PositionUtils.getRotationNameShort(data.getRotation());
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.rotation", rotationStr);

        String mirrorStr = PositionUtils.getMirrorName(data.getMirror());
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.mirror", mirrorStr);

        BlockPos o = data.getOrigin();
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.origin",
                                 o.getX(), o.getY(), o.getZ());

        long time = data.getLastSaveTime();

        if (time > 0)
        {
            StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.last_saved",
                                     BaseFileBrowserWidget.DATE_FORMAT.format(new Date(time)));
        }

        LabelWidget label = new LabelWidget();
        label.setPosition(x, y);
        label.setLineHeight(12);
        label.setLabelStyledTextLines(lines);

        return label;
    }
}
