package litematica.gui.widget.list.entry;

import java.util.Comparator;
import java.util.Optional;
import com.google.common.collect.ImmutableList;

import net.minecraft.item.ItemStack;

import malilib.config.value.SortDirection;
import malilib.gui.util.ElementOffset;
import malilib.gui.util.ScreenContext;
import malilib.gui.widget.BaseModelWidget;
import malilib.gui.widget.BlockModelWidget;
import malilib.gui.widget.InteractableWidget;
import malilib.gui.widget.ItemStackWidget;
import malilib.gui.widget.button.GenericButton;
import malilib.gui.widget.list.DataListWidget;
import malilib.gui.widget.list.ListEntryWidgetInitializer;
import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.gui.widget.list.header.DataColumn;
import malilib.render.text.StyledTextLine;
import malilib.util.StringUtils;
import litematica.config.Configs;
import litematica.gui.widget.SchematicVerifierBlockInfoWidget;
import litematica.materials.MaterialCache;
import litematica.schematic.verifier.BlockStatePair;
import litematica.schematic.verifier.BlockStatePairCount;
import litematica.schematic.verifier.SchematicVerifier;
import litematica.schematic.verifier.VerifierResultType;

public class SchematicVerifierResultEntryWidget extends BaseDataListEntryWidget<BlockStatePairCount>
{
    public static final DataColumn<BlockStatePairCount> EXPECTED_COLUMN =
            new DataColumn<>("litematica.label.schematic_verifier.column.expected",
                             Comparator.comparing(BlockStatePairCount::getExpectedBlockDisplayName));

    public static final DataColumn<BlockStatePairCount> FOUND_COLUMN =
            new DataColumn<>("litematica.label.schematic_verifier.column.found",
                             Comparator.comparing(BlockStatePairCount::getFoundBlockDisplayName));

    public static final DataColumn<BlockStatePairCount> COUNT_COLUMN =
            new DataColumn<>("litematica.label.schematic_verifier.column.count",
                             Comparator.comparingInt(BlockStatePairCount::getCount),
                             SortDirection.DESCENDING);

    public static final ImmutableList<DataColumn<BlockStatePairCount>> COLUMNS
            = ImmutableList.of(EXPECTED_COLUMN, FOUND_COLUMN, COUNT_COLUMN);

    protected final SchematicVerifier verifier;
    protected final GenericButton ignoreButton;
    protected final BaseModelWidget expectedModelWidget;
    protected final BaseModelWidget foundModelWidget;
    protected final StyledTextLine expectedNameText;
    protected final StyledTextLine foundNameText;
    protected final StyledTextLine countText;
    protected boolean selected;
    protected int foundColumnX;
    protected int countColumnRight;

    public SchematicVerifierResultEntryWidget(BlockStatePairCount data,
                                              DataListEntryWidgetData constructData,
                                              SchematicVerifier verifier)
    {
        super(data, constructData);

        this.verifier = verifier;
        VerifierResultType type = data.getPair().type;
        BlockStatePair pair = data.getPair();

        this.canReceiveMouseClicks = true;
        this.ignoreButton = GenericButton.create(18, "litematica.button.schematic_verifier.ignore", this::ignoreEntry);
        this.countText = StyledTextLine.parseFirstLine(String.valueOf(data.getCount()));

        if (Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue())
        {
            this.expectedModelWidget = new BlockModelWidget(pair.expectedState);
            this.foundModelWidget = new BlockModelWidget(pair.foundState);
            this.expectedNameText = StyledTextLine.parseFirstLine(data.getExpectedBlockDisplayName());
            this.foundNameText = StyledTextLine.parseFirstLine(data.getFoundBlockDisplayName());
        }
        else
        {
            ItemStack expectedStack = MaterialCache.getInstance().getItemForDisplayNameForState(pair.expectedState);
            ItemStack foundStack = MaterialCache.getInstance().getItemForDisplayNameForState(pair.foundState);
            this.expectedModelWidget = new ItemStackWidget(expectedStack);
            this.foundModelWidget = new ItemStackWidget(foundStack);
            this.expectedNameText = StyledTextLine.parseFirstLine(expectedStack.getDisplayName());
            this.foundNameText = StyledTextLine.parseFirstLine(foundStack.getDisplayName());
        }

        int color = 0xFF202020;
        this.expectedModelWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, color);
        this.foundModelWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, color);

        this.textSettings.setTextColor(type.getTextColor());
        this.selected = this.verifier.isPairSelected(pair);

        this.hoverInfoWidget = new SchematicVerifierBlockInfoWidget(type, pair.expectedState, pair.foundState);
    }

    @Override
    public void reAddSubWidgets()
    {
        super.reAddSubWidgets();

        this.addWidget(this.expectedModelWidget);
        this.addWidget(this.foundModelWidget);
        this.addWidget(this.ignoreButton);
    }

    @Override
    public void updateSubWidgetPositions()
    {
        super.updateSubWidgetPositions();

        int x = this.getX();
        this.expectedModelWidget.setX(x + 2);
        this.expectedModelWidget.centerVerticallyInside(this);
        this.foundModelWidget.setX(x + this.foundColumnX);
        this.foundModelWidget.centerVerticallyInside(this);

        this.ignoreButton.setRight(this.getRight() - 2);
        this.ignoreButton.centerVerticallyInside(this);
    }

    @Override
    protected boolean isSelected()
    {
        return this.selected;
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton) == false)
        {
            this.verifier.togglePairSelected(this.data.getPair());
            this.selected = this.verifier.isPairSelected(this.data.getPair());
        }

        return true;
    }

    @Override
    public void renderAt(int x, int y, float z, ScreenContext ctx)
    {
        super.renderAt(x, y, z, ctx);

        int textY = y + ElementOffset.getCenteredElementOffset(this.getHeight(), 8);
        int white = 0xFFFFFFFF;
        int color = this.textSettings.getTextColor();
        z += 0.0125f;

        this.renderTextLine(x + 22, textY, z, white, true, this.expectedNameText, ctx);
        this.renderTextLine(x + this.foundColumnX + 22, textY, z, white, true, this.foundNameText, ctx);
        this.renderTextLineRightAligned(x + this.countColumnRight, textY, z, color, true, this.countText, ctx);
    }

    protected void ignoreEntry()
    {
        this.scheduleTask(() -> {
            this.verifier.ignoreStatePair(this.data.getPair());
            this.listWidget.refreshEntries();
        });
    }

    public static class WidgetInitializer implements ListEntryWidgetInitializer<BlockStatePairCount>
    {
        @Override
        public void onListContentsRefreshed(DataListWidget<BlockStatePairCount> dataListWidget, int entryWidgetWidth)
        {
            int expectedNameColumnWidth = getRenderWidth(EXPECTED_COLUMN.getName(), 60);
            int foundNameColumnWidth = getRenderWidth(FOUND_COLUMN.getName(), 60);
            int countColumnWidth = getRenderWidth(EXPECTED_COLUMN.getName(), 40);

            if (Configs.Visuals.SCHEMATIC_VERIFIER_BLOCK_MODELS.getBooleanValue())
            {
                for (BlockStatePairCount entry : dataListWidget.getNonFilteredDataList())
                {
                    expectedNameColumnWidth = Math.max(expectedNameColumnWidth, StringUtils.getStringWidth(entry.getExpectedBlockDisplayName()));
                    foundNameColumnWidth = Math.max(foundNameColumnWidth, StringUtils.getStringWidth(entry.getFoundBlockDisplayName()));
                }
            }
            else
            {
                for (BlockStatePairCount entry : dataListWidget.getNonFilteredDataList())
                {
                    ItemStack expectedStack = MaterialCache.getInstance().getItemForDisplayNameForState(entry.getPair().expectedState);
                    ItemStack foundStack = MaterialCache.getInstance().getItemForDisplayNameForState(entry.getPair().foundState);
                    expectedNameColumnWidth = Math.max(expectedNameColumnWidth, StringUtils.getStringWidth(expectedStack.getDisplayName()));
                    foundNameColumnWidth = Math.max(foundNameColumnWidth, StringUtils.getStringWidth(foundStack.getDisplayName()));
                }
            }

            int extra = 24; // leave space for the sort direction icon and padding
            expectedNameColumnWidth += 32;
            foundNameColumnWidth += 32;
            countColumnWidth += extra;
            int relativeStartX = 2;

            EXPECTED_COLUMN.setRelativeStartX(relativeStartX);
            EXPECTED_COLUMN.setWidth(expectedNameColumnWidth);
            relativeStartX += expectedNameColumnWidth + 2;

            FOUND_COLUMN.setRelativeStartX(relativeStartX);
            FOUND_COLUMN.setWidth(foundNameColumnWidth);
            relativeStartX += foundNameColumnWidth + 2;

            COUNT_COLUMN.setRelativeStartX(relativeStartX);
            COUNT_COLUMN.setWidth(countColumnWidth);
        }

        @Override
        public void applyToEntryWidgets(DataListWidget<BlockStatePairCount> dataListWidget)
        {
            int foundColumnX = FOUND_COLUMN.getRelativeStartX() + 2;
            int countColumnRight = COUNT_COLUMN.getRelativeRight() - 3;

            for (InteractableWidget w : dataListWidget.getEntryWidgetList())
            {
                if (w instanceof SchematicVerifierResultEntryWidget)
                {
                    SchematicVerifierResultEntryWidget widget = (SchematicVerifierResultEntryWidget) w;
                    widget.foundColumnX = foundColumnX;
                    widget.countColumnRight = countColumnRight;
                    widget.updateSubWidgetPositions();
                }
            }
        }

        protected static int getRenderWidth(Optional<StyledTextLine> optional, int minWidth)
        {
            int width = optional.isPresent() ? optional.get().renderWidth : minWidth;
            return Math.max(width, minWidth);
        }
    }
}
