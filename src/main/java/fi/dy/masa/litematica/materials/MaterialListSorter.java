package fi.dy.masa.litematica.materials;

import java.util.Comparator;
import fi.dy.masa.litematica.materials.MaterialListBase.SortCriteria;

public class MaterialListSorter implements Comparator<MaterialListEntry>
{
    private final MaterialListBase materialList;

    public MaterialListSorter(MaterialListBase materialList)
    {
        this.materialList = materialList;
    }

    @Override
    public int compare(MaterialListEntry entry1, MaterialListEntry entry2)
    {
        boolean reverse = this.materialList.getSortInReverse();
        SortCriteria sortCriteria = this.materialList.getSortCriteria();
        int nameCompare = entry1.getStack().getName().getString().compareTo(entry2.getStack().getName().getString());

        if (sortCriteria == SortCriteria.COUNT_TOTAL)
        {
            return entry1.getCountTotal() == entry2.getCountTotal() ? nameCompare : ((entry1.getCountTotal() > entry2.getCountTotal()) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_MISSING)
        {
            return entry1.getCountMissing() == entry2.getCountMissing() ? nameCompare : ((entry1.getCountMissing() > entry2.getCountMissing()) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_AVAILABLE)
        {
            return entry1.getCountAvailable() == entry2.getCountAvailable() ? nameCompare : ((entry1.getCountAvailable() > entry2.getCountAvailable()) != reverse ? -1 : 1);
        }

        return reverse == false ? nameCompare * -1 : nameCompare;
    }
}
