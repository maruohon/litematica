package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.schematic.verifier.VerifierResultSorter;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetListSchematicVerificationResults extends WidgetListBase<BlockMismatchEntry, WidgetSchematicVerificationResult>
{
    private static int lastScrollbarPosition;

    private final GuiSchematicVerifier guiSchematicVerifier;
    private final VerifierResultSorter sorter;
    private boolean scrollbarRestored;

    public WidgetListSchematicVerificationResults(int x, int y, int width, int height, GuiSchematicVerifier parent)
    {
        super(x, y, width, height, parent);

        this.browserEntryHeight = 22;
        this.guiSchematicVerifier = parent;
        this.allowMultiSelection = true;
        this.sorter = new VerifierResultSorter(parent.getPlacement().getSchematicVerifier());
    }

    @Override
    public void drawContents(DrawContext drawContext, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(drawContext, mouseX, mouseY, partialTicks);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected void offsetSelectionOrScrollbar(int amount, boolean changeSelection)
    {
        super.offsetSelectionOrScrollbar(amount, changeSelection);
        lastScrollbarPosition = this.scrollBar.getValue();
    }

    @Override
    protected WidgetSchematicVerificationResult createHeaderWidget(int x, int y, int listIndexStart, int usableHeight, int usedHeight)
    {
        int height = this.browserEntryHeight;

        if ((usedHeight + height) > usableHeight)
        {
            return null;
        }

        MismatchType type = this.guiSchematicVerifier.getResultMode();
        String strExpected = TXT_BOLD + StringUtils.translate(WidgetSchematicVerificationResult.HEADER_EXPECTED) + TXT_RST;
        BlockMismatchEntry entry;

        if (type != MismatchType.CORRECT_STATE)
        {
            String strFound = TXT_WHITE + TXT_BOLD + StringUtils.translate(WidgetSchematicVerificationResult.HEADER_FOUND) + TXT_RST;
            entry = new BlockMismatchEntry(strExpected, strFound);
        }
        else
        {
            entry = new BlockMismatchEntry(strExpected, "");
        }

        return this.createListEntryWidget(x, y, listIndexStart, true, entry);
    }

    @Override
    protected void refreshBrowserEntries()
    {
        this.listContents.clear();

        MismatchType type = this.guiSchematicVerifier.getResultMode();

        if (type == MismatchType.ALL)
        {
            this.addEntriesForType(MismatchType.WRONG_BLOCK);
            this.addEntriesForType(MismatchType.WRONG_STATE);
            this.addEntriesForType(MismatchType.EXTRA);
            this.addEntriesForType(MismatchType.MISSING);
        }
        else
        {
            this.addEntriesForType(type);
        }

        this.reCreateListEntryWidgets();

        if (this.scrollbarRestored == false && lastScrollbarPosition <= this.scrollBar.getMaxValue())
        {
            // This needs to happen after the setMaxValue() has been called in reCreateListEntryWidgets()
            this.scrollBar.setValue(lastScrollbarPosition);
            this.scrollbarRestored = true;
            this.reCreateListEntryWidgets();
        }
    }

    private void addEntriesForType(MismatchType type)
    {
        String title = type.getFormattingCode() + type.getDisplayname() + TXT_RST;
        this.listContents.add(new BlockMismatchEntry(type, title));
        List<BlockMismatch> list;

        if (type == MismatchType.CORRECT_STATE)
        {
            Object2IntOpenHashMap<BlockState> counts = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getCorrectStates();
            Object2IntOpenHashMap<ItemType> itemCounts = new Object2IntOpenHashMap<>();
            Object2ObjectOpenHashMap<ItemType, BlockState> states = new Object2ObjectOpenHashMap<>();

            for (BlockState state : counts.keySet())
            {
                if (state.isAir())
                {
                    continue;
                }

                ItemStack stack = ItemUtils.getItemForState(state);
                ItemType itemType = new ItemType(stack, true, false);

                if (itemCounts.containsKey(itemType) == false)
                {
                    states.put(itemType, state);
                }

                itemCounts.addTo(itemType, counts.getInt(state));
            }

            list = new ArrayList<>();

            for (ItemType itemType : itemCounts.keySet())
            {
                BlockState state = states.get(itemType);
                BlockMismatch mismatch = new BlockMismatch(MismatchType.CORRECT_STATE, state, state, itemCounts.getInt(itemType));
                list.add(mismatch);
            }
        }
        else
        {
            list = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getMismatchOverviewFor(type);
        }

        Collections.sort(list, this.sorter);

        for (BlockMismatch mismatch : list)
        {
            this.listContents.add(new BlockMismatchEntry(type, mismatch));
        }
    }

    @Override
    protected WidgetSchematicVerificationResult createListEntryWidget(int x, int y, int listIndex, boolean isOdd, BlockMismatchEntry entry)
    {
        return new WidgetSchematicVerificationResult(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry),
                isOdd, this, this.guiSchematicVerifier, entry, listIndex);
    }
}
