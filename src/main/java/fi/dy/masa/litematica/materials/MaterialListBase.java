package fi.dy.masa.litematica.materials;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.malilib.util.JsonUtils;

public abstract class MaterialListBase
{
    protected ImmutableList<MaterialListEntry> materialList = ImmutableList.of();
    protected SortCriteria sortCriteria = SortCriteria.COUNT_TOTAL;
    protected BlockInfoListType materialListType = BlockInfoListType.ALL;
    protected boolean reverse;
    protected boolean hideAvailable;

    protected abstract List<MaterialListEntry> createMaterialListEntries();

    public abstract String getDisplayName();

    public ImmutableList<MaterialListEntry> getMaterialsAll()
    {
        return this.materialList;
    }

    public List<MaterialListEntry> getMaterialsFiltered()
    {
        if (this.hideAvailable)
        {
            ArrayList<MaterialListEntry> list = new ArrayList<>();

            for (MaterialListEntry entry : this.materialList)
            {
                if (entry.getCountAvailable() < entry.getCountMissing())
                {
                    list.add(entry);
                }
            }

            return list;
        }

        return this.materialList;
    }

    public void refreshMaterialList()
    {
        this.materialList = ImmutableList.copyOf(this.createMaterialListEntries());
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

    public void setSortCriteria(SortCriteria criteria)
    {
        if (this.sortCriteria == criteria)
        {
            this.reverse = ! this.reverse;
        }
        else
        {
            this.sortCriteria = criteria;
            this.reverse = false;
        }
    }

    public void setHideAvailable(boolean hideAvailable)
    {
        this.hideAvailable = hideAvailable;
    }

    public BlockInfoListType getMaterialListType()
    {
        return this.materialListType;
    }

    public void setMaterialListType(BlockInfoListType type)
    {
        this.materialListType = type;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("type", new JsonPrimitive(this.getMaterialListType().getStringValue()));
        obj.add("sort_criteria", new JsonPrimitive(this.sortCriteria.name()));
        obj.add("sort_reverse", new JsonPrimitive(this.reverse));
        obj.add("hide_available", new JsonPrimitive(this.hideAvailable));

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "type"))
        {
            this.setMaterialListType(BlockInfoListType.fromStringStatic(JsonUtils.getString(obj, "type")));
        }

        if (JsonUtils.hasString(obj, "sort_criteria"))
        {
            this.sortCriteria = SortCriteria.fromStringStatic(JsonUtils.getString(obj, "sort_criteria"));
        }

        this.reverse = JsonUtils.getBooleanOrDefault(obj, "sort_reverse", false);
        this.hideAvailable = JsonUtils.getBooleanOrDefault(obj, "hide_available", false);
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
