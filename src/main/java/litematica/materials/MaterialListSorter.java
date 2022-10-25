package litematica.materials;

import java.util.Comparator;

import litematica.materials.MaterialListBase.SortCriteria;

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
        int nameCompare = entry1.getStack().getDisplayName().compareTo(entry2.getStack().getDisplayName());

        if (sortCriteria == SortCriteria.COUNT_TOTAL)
        {
            return entry1.getTotalCount() == entry2.getTotalCount() ? nameCompare : ((entry1.getTotalCount() > entry2.getTotalCount()) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_MISSING)
        {
            return entry1.getMissingCount() == entry2.getMissingCount() ? nameCompare : ((entry1.getMissingCount() > entry2.getMissingCount()) != reverse ? -1 : 1);
        }
        else if (sortCriteria == SortCriteria.COUNT_AVAILABLE)
        {
            return entry1.getAvailableCount() == entry2.getAvailableCount() ? nameCompare : ((entry1.getAvailableCount() > entry2.getAvailableCount()) != reverse ? -1 : 1);
        }

        return reverse == false ? nameCompare * -1 : nameCompare;
    }
}
