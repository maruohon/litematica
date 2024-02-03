package litematica.gui.widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;

import malilib.gui.widget.ContainerWidget;
import malilib.gui.widget.LabelWidget;
import malilib.gui.widget.list.BaseFileBrowserWidget.DirectoryEntry;
import malilib.overlay.message.MessageHelpers;
import malilib.render.text.StyledTextLine;
import malilib.util.position.BlockPos;
import litematica.gui.util.SchematicPlacementInfoCache;
import litematica.schematic.placement.SchematicPlacement;

public class SavedSchematicPlacementInfoWidget extends ContainerWidget
{
    protected final SchematicPlacementInfoCache infoCache = new SchematicPlacementInfoCache();
    protected final LabelWidget infoLabelWidget;
    protected SimpleDateFormat dateFormat;
    @Nullable protected SchematicPlacement currentInfo;

    public SavedSchematicPlacementInfoWidget(int width, int height)
    {
        super(width, height);

        this.dateFormat = AbstractSchematicInfoWidget.createDateFormat();
        this.infoLabelWidget = new LabelWidget();
        this.infoLabelWidget.setLineHeight(12);

        this.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xC0000000);
        this.getBorderRenderer().getNormalSettings().setBorderWidthAndColor(1, 0xFFC0C0C0);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidgetIf(this.infoLabelWidget, this.currentInfo != null);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        this.infoLabelWidget.setPosition(this.getX() + 4, this.getY() + 4);
    }

    @Nullable
    public SchematicPlacement getSelectedPlacementInfo()
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

        if (this.currentInfo != null)
        {
            this.updateInfoLabelWidget(this.currentInfo);
        }

        this.reAddSubWidgets();
    }

    public void clearInfoCache()
    {
        this.infoCache.clearCache();
    }

    protected void updateInfoLabelWidget(SchematicPlacement placement)
    {
        List<StyledTextLine> lines = new ArrayList<>();

        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.region_count",
                                 placement.getSubRegionCount());

        String stateStr = MessageHelpers.getOnOffColored(placement.isEnabled(), true);
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.placement_state", stateStr);

        stateStr = MessageHelpers.getYesNoColored(placement.isRegionPlacementModified(), false);
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.modified", stateStr);

        String rotationStr = placement.getRotation().getDisplayName();
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.rotation", rotationStr);

        String mirrorStr = placement.getMirror().getDisplayName();
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.mirror", mirrorStr);

        BlockPos o = placement.getPosition();
        StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.origin",
                                 o.getX(), o.getY(), o.getZ());

        long time = placement.getLastSaveTime();

        if (time > 0)
        {
            StyledTextLine.translate(lines, "litematica.label.saved_placement.info_widget.last_saved",
                                     this.dateFormat.format(new Date(time)));
        }

        this.infoLabelWidget.setLines(lines);
    }
}
