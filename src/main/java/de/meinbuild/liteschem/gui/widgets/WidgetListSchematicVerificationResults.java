package de.meinbuild.liteschem.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.meinbuild.liteschem.schematic.verifier.SchematicVerifier;
import de.meinbuild.liteschem.schematic.verifier.VerifierResultSorter;
import de.meinbuild.liteschem.util.ItemUtils;
import de.meinbuild.liteschem.gui.GuiSchematicVerifier;
import de.meinbuild.liteschem.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.util.ItemType;
import fi.dy.masa.malilib.util.StringUtils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

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
        this.setParent(parent);
    }

    @Override
    public void drawContents(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.drawContents(matrixStack, mouseX, mouseY, partialTicks);
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

        SchematicVerifier.MismatchType type = this.guiSchematicVerifier.getResultMode();
        String strExpected = TXT_BOLD + StringUtils.translate(WidgetSchematicVerificationResult.HEADER_EXPECTED) + TXT_RST;
        BlockMismatchEntry entry;

        if (type != SchematicVerifier.MismatchType.CORRECT_STATE)
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

        SchematicVerifier.MismatchType type = this.guiSchematicVerifier.getResultMode();

        if (type == SchematicVerifier.MismatchType.ALL)
        {
            this.addEntriesForType(SchematicVerifier.MismatchType.WRONG_BLOCK);
            this.addEntriesForType(SchematicVerifier.MismatchType.WRONG_STATE);
            this.addEntriesForType(SchematicVerifier.MismatchType.EXTRA);
            this.addEntriesForType(SchematicVerifier.MismatchType.MISSING);
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

    private void addEntriesForType(SchematicVerifier.MismatchType type)
    {
        String title = type.getFormattingCode() + type.getDisplayname() + TXT_RST;
        this.listContents.add(new BlockMismatchEntry(type, title));
        List<SchematicVerifier.BlockMismatch> list;

        if (type == SchematicVerifier.MismatchType.CORRECT_STATE)
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
                ItemType itemType = new ItemType(stack, false, true);

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
                SchematicVerifier.BlockMismatch mismatch = new SchematicVerifier.BlockMismatch(SchematicVerifier.MismatchType.CORRECT_STATE, state, state, itemCounts.getInt(itemType));
                list.add(mismatch);
            }
        }
        else
        {
            list = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getMismatchOverviewFor(type);
        }

        Collections.sort(list, this.sorter);

        for (SchematicVerifier.BlockMismatch mismatch : list)
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
