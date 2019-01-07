package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import fi.dy.masa.litematica.data.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.util.ItemType;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public class WidgetListSchematicVerificationResults extends WidgetListBase<BlockMismatchEntry, WidgetSchematicVerificationResult>
{
    private final GuiSchematicVerifier guiSchematicVerifier;

    public WidgetListSchematicVerificationResults(int x, int y, int width, int height, GuiSchematicVerifier parent)
    {
        super(x, y, width, height, parent);

        this.browserEntryHeight = 22;
        this.guiSchematicVerifier = parent;
        this.setParent(parent);
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        MismatchType type = this.guiSchematicVerifier.getResultMode();

        if (type != MismatchType.CORRECT_STATE)
        {
            String strExpected = TXT_WHITE + TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.expected") + TXT_RST;
            String strFound = TXT_WHITE + TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.found") + TXT_RST;
            this.listContents.add(new BlockMismatchEntry(strExpected, strFound));
        }
        else
        {
            String strExpected = TXT_WHITE + TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.expected") + TXT_RST;
            this.listContents.add(new BlockMismatchEntry(strExpected, ""));
        }

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
    }

    private void addEntriesForType(MismatchType type)
    {
        String title = type.getFormattingCode() + type.getDisplayname() + TXT_RST;
        this.listContents.add(new BlockMismatchEntry(type, title));

        if (type == MismatchType.CORRECT_STATE)
        {
            Object2IntOpenHashMap<IBlockState> counts = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getCorrectStates();
            Object2IntOpenHashMap<ItemType> itemCounts = new Object2IntOpenHashMap<>();
            Object2ObjectOpenHashMap<ItemType, IBlockState> states = new Object2ObjectOpenHashMap<>();

            for (IBlockState state : counts.keySet())
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

            List<BlockMismatchEntry> list = new ArrayList<>();

            for (ItemType itemType : itemCounts.keySet())
            {
                IBlockState state = states.get(itemType);
                BlockMismatch mismatch = new BlockMismatch(MismatchType.CORRECT_STATE, state, state, itemCounts.getInt(itemType));
                list.add(new BlockMismatchEntry(type, mismatch));
            }

            Collections.sort(list, new Comparator<BlockMismatchEntry>()
            {
                @Override
                public int compare(BlockMismatchEntry o1, BlockMismatchEntry o2)
                {
                    return o1.blockMismatch.count > o2.blockMismatch.count ? -1 : (o1.blockMismatch.count < o2.blockMismatch.count ? 1 : 0);
                }
            });

            this.listContents.addAll(list);
        }
        else
        {
            List<BlockMismatch> list = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getMismatchOverviewFor(type);

            for (BlockMismatch entry : list)
            {
                this.listContents.add(new BlockMismatchEntry(type, entry));
            }
        }
    }

    @Override
    protected WidgetSchematicVerificationResult createListEntryWidget(int x, int y, int listIndex, boolean isOdd, BlockMismatchEntry entry)
    {
        return new WidgetSchematicVerificationResult(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd, entry, this.guiSchematicVerifier);
    }
}
