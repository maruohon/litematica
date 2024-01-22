package litematica.materials;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import malilib.config.value.BaseOptionListConfigValue;
import malilib.listener.TaskCompletionListener;
import malilib.util.MathUtils;
import malilib.util.data.json.JsonUtils;
import malilib.util.datadump.DataDump;
import litematica.util.value.BlockInfoListType;

public abstract class MaterialListBase implements IMaterialList
{
    protected final MaterialListHudRenderer hudRenderer = new MaterialListHudRenderer(this);
    protected final Set<MaterialListEntry> ignored = new HashSet<>();
    protected final List<MaterialListEntry> materialListPreFiltered = new ArrayList<>();
    protected final List<MaterialListEntry> materialListFiltered = new ArrayList<>();
    protected ImmutableList<MaterialListEntry> materialListAll = ImmutableList.of();
    @Nullable protected TaskCompletionListener completionListener;
    protected SortCriteria sortCriteria = SortCriteria.COUNT_TOTAL;
    protected BlockInfoListType materialListType = BlockInfoListType.ALL;
    protected boolean reverse;
    protected boolean hideAvailable;
    protected long multiplier = 1L;
    protected long countTotal;
    protected long countMissing;
    protected long countMismatched;

    public abstract String getName();

    public abstract String getTitle();

    public boolean supportsRenderLayers()
    {
        return false;
    }

    /**
     * @return Whether this material list is made based on a schematic placement,
     * and thus should be cleared on dimension changes and when the currently
     * selected placement is changed by any means.
     */
    public boolean isForPlacement()
    {
        return false;
    }

    public MaterialListHudRenderer getHudRenderer()
    {
        return this.hudRenderer;
    }

    public ImmutableList<MaterialListEntry> getAllMaterials()
    {
        return this.materialListAll;
    }

    public List<MaterialListEntry> getFilteredMaterials(boolean refresh)
    {
        if (this.hideAvailable)
        {
            return this.getMissingMaterials(refresh);
        }

        return this.materialListPreFiltered;
    }

    public List<MaterialListEntry> getMissingMaterials(boolean refresh)
    {
        if (refresh)
        {
            this.reCreateFilteredList();
        }

        return this.materialListFiltered;
    }

    public void setCompletionListener(TaskCompletionListener listener)
    {
        this.completionListener = listener;
    }

    public void reCreateFilteredList()
    {
        this.materialListFiltered.clear();

        for (int i = 0; i < this.materialListPreFiltered.size(); ++i)
        {
            MaterialListEntry entry = this.materialListPreFiltered.get(i);
            long countMissing = this.getMultipliedMissingCount(entry);

            if (entry.getAvailableCount() < countMissing)
            {
                this.materialListFiltered.add(entry);
            }
            // Remove entries that have been seen as available at least at one point
            // (for example when gathering resources to a staging area)
            else if (this.hideAvailable)
            {
                this.materialListPreFiltered.remove(i);
                --i;
            }
        }
    }

    public long getMultipliedMissingCount(MaterialListEntry entry)
    {
        long multiplier = this.getMultiplier();
        long missing = entry.getMissingCount();

        if (multiplier > 1L)
        {
            long total = entry.getTotalCount();
            return (multiplier - 1L) * total + missing;
        }

        return missing;
    }

    public void ignoreEntry(MaterialListEntry entry)
    {
        this.ignored.add(entry);
        this.materialListPreFiltered.remove(entry);
        this.reCreateFilteredList();
    }

    public void clearIgnored()
    {
        this.ignored.clear();
        this.refreshPreFilteredList();
        this.reCreateFilteredList();
    }

    /**
     * Re-creates the all-materials list from the schematic or placement or area
     * by starting a new task, if applicable.
     */
    public abstract void reCreateMaterialList();

    @Override
    public void setMaterialListEntries(List<MaterialListEntry> list)
    {
        this.materialListAll = ImmutableList.copyOf(list);
        this.refreshPreFilteredList();
        this.updateCounts();

        if (this.completionListener != null)
        {
            this.completionListener.onTaskCompleted();
        }
    }

    @Override
    public BlockInfoListType getMaterialListType()
    {
        return this.materialListType;
    }

    /**
     * Resets the pre-filtered materials list to the all materials list
     */
    public void refreshPreFilteredList()
    {
        this.materialListPreFiltered.clear();
        this.materialListPreFiltered.addAll(this.materialListAll);
        this.materialListPreFiltered.removeAll(this.ignored);
    }

    public SortCriteria getSortCriteria()
    {
        return this.sortCriteria;
    }

    public boolean getSortInReverse()
    {
        return this.reverse;
    }

    public boolean getHideAvailable()
    {
        return this.hideAvailable;
    }

    public long getMultiplier()
    {
        return this.multiplier;
    }

    public void setSortCriteria(SortCriteria criteria)
    {
        if (this.sortCriteria == criteria)
        {
            this.reverse = ! this.reverse;
        }
        else
        {
            this.sortCriteria = criteria;
            this.reverse = criteria == SortCriteria.NAME;
        }
    }

    public void setHideAvailable(boolean hideAvailable)
    {
        this.hideAvailable = hideAvailable;
    }

    public void setMultiplier(int multiplier)
    {
        this.multiplier = MathUtils.clamp(multiplier, 1, Integer.MAX_VALUE);
    }

    public void updateCounts()
    {
        this.countTotal = 0;
        this.countMissing = 0;
        this.countMismatched = 0;

        for (MaterialListEntry entry : this.materialListAll)
        {
            this.countTotal += entry.getTotalCount();
            this.countMissing += entry.getMissingCount();
            this.countMismatched += entry.getCountMismatched();
        }
    }

    public long getCountTotal()
    {
        return this.countTotal;
    }

    public long getCountMissing()
    {
        return this.countMissing;
    }

    public long getCountMismatched()
    {
        return this.countMismatched;
    }

    public void setMaterialListType(BlockInfoListType type)
    {
        this.materialListType = type;
    }

    public DataDump getMaterialListDump(DataDump.Format format)
    {
        DataDump dump = new DataDump(4, format);
        long multiplier = this.getMultiplier();

        ArrayList<MaterialListEntry> list = new ArrayList<>(this.getFilteredMaterials(false));
        list.sort(new MaterialListSorter(this));

        for (MaterialListEntry entry : list)
        {
            long total = entry.getTotalCount() * multiplier;
            long missing = multiplier > 1L ? total : entry.getMissingCount();
            long available = entry.getAvailableCount();

            dump.addData(entry.getStack().getDisplayName(),
                         String.valueOf(total),
                         String.valueOf(missing),
                         String.valueOf(available));
        }

        String titleTotal = multiplier > 1L ? String.format("Total (x%d)", multiplier) : "Total";

        dump.addTitle("Item", titleTotal, "Missing", "Available");
        dump.addHeader(this.getTitle());
        dump.setColumnProperties(1, DataDump.Alignment.RIGHT, true); // total
        dump.setColumnProperties(2, DataDump.Alignment.RIGHT, true); // missing
        dump.setColumnProperties(3, DataDump.Alignment.RIGHT, true); // available
        dump.setSort(false);
        dump.setUseColumnSeparator(true);

        return dump;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("type", new JsonPrimitive(this.getMaterialListType().getName()));
        obj.add("sort_criteria", new JsonPrimitive(this.sortCriteria.name()));
        obj.add("sort_reverse", new JsonPrimitive(this.reverse));
        obj.add("hide_available", new JsonPrimitive(this.hideAvailable));
        obj.add("multiplier", new JsonPrimitive(this.getMultiplier()));

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "type"))
        {
            this.setMaterialListType(BaseOptionListConfigValue.findValueByName(JsonUtils.getString(obj, "type"), BlockInfoListType.VALUES));
        }

        if (JsonUtils.hasString(obj, "sort_criteria"))
        {
            this.sortCriteria = SortCriteria.fromStringStatic(JsonUtils.getString(obj, "sort_criteria"));
        }

        this.reverse = JsonUtils.getBooleanOrDefault(obj, "sort_reverse", false);
        this.hideAvailable = JsonUtils.getBooleanOrDefault(obj, "hide_available", false);
        this.setMultiplier(JsonUtils.getIntegerOrDefault(obj, "multiplier", 1));
    }

    public enum SortCriteria
    {
        NAME,
        COUNT_TOTAL,
        COUNT_MISSING,
        COUNT_AVAILABLE;

        public static SortCriteria fromStringStatic(String name)
        {
            for (SortCriteria mode : SortCriteria.values())
            {
                if (mode.name().equalsIgnoreCase(name))
                {
                    return mode;
                }
            }

            return SortCriteria.COUNT_TOTAL;
        }
    }
}
