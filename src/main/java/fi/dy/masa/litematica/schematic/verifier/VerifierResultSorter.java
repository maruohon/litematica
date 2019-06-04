package fi.dy.masa.litematica.schematic.verifier;

import java.util.Comparator;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.SortCriteria;
import fi.dy.masa.litematica.util.ItemUtils;
import net.minecraft.block.state.IBlockState;

public class VerifierResultSorter implements Comparator<BlockMismatch>
{
    private final SchematicVerifier verifier;

    public VerifierResultSorter(SchematicVerifier verifier)
    {
        this.verifier = verifier;
    }

    @Override
    public int compare(BlockMismatch entry1, BlockMismatch entry2)
    {
        boolean reverse = this.verifier.getSortInReverse();
        SortCriteria sortCriteria = this.verifier.getSortCriteria();

        if (sortCriteria == SortCriteria.COUNT)
        {
            int count1 = entry1.count;
            int count2 = entry2.count;

            if (count1 == count2)
            {
                String name1 = ItemUtils.getItemForState(entry1.stateExpected).getDisplayName().getString();
                String name2 = ItemUtils.getItemForState(entry2.stateExpected).getDisplayName().getString();
                return name1.compareTo(name2);
            }

            return (count1 > count2) != reverse ? -1 : 1;
        }
        else
        {
            IBlockState state1_1, state1_2, state2_1, state2_2;

            if (sortCriteria == SortCriteria.NAME_EXPECTED)
            {
                state1_1 = entry1.stateExpected;
                state1_2 = entry2.stateExpected;
                state2_1 = entry1.stateFound;
                state2_2 = entry2.stateFound;
            }
            else
            {
                state2_1 = entry1.stateExpected;
                state2_2 = entry2.stateExpected;
                state1_1 = entry1.stateFound;
                state1_2 = entry2.stateFound;
            }

            String name1_1 = ItemUtils.getItemForState(state1_1).getDisplayName().getString();
            String name1_2 = ItemUtils.getItemForState(state1_2).getDisplayName().getString();
            int res = name1_1.compareTo(name1_2);

            if (res != 0)
            {
                return reverse == false ? res * -1 : res;
            }

            String name2_1 = ItemUtils.getItemForState(state2_1).getDisplayName().getString();
            String name2_2 = ItemUtils.getItemForState(state2_2).getDisplayName().getString();
            res = name2_1.compareTo(name2_2);

            return reverse == false ? res * -1 : res;
        }
    }
}
