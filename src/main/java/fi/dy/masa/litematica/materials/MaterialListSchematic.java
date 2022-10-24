package fi.dy.masa.litematica.materials;

import java.util.Collection;
import com.google.common.collect.ImmutableList;

import malilib.util.StringUtils;
import fi.dy.masa.litematica.schematic.ISchematic;

public class MaterialListSchematic extends MaterialListBase
{
    private final ISchematic schematic;
    private final ImmutableList<String> regions;

    public MaterialListSchematic(ISchematic schematic, boolean reCreate)
    {
        this(schematic, schematic.getRegionNames(), reCreate);
    }

    public MaterialListSchematic(ISchematic schematic, Collection<String> subRegions, boolean reCreate)
    {
        super();

        this.schematic = schematic;
        this.regions = ImmutableList.copyOf(subRegions);

        if (reCreate)
        {
            this.reCreateMaterialList();
        }
    }

    @Override
    public void reCreateMaterialList()
    {
        this.materialListAll = ImmutableList.copyOf(MaterialListUtils.createMaterialListFor(this.schematic, this.regions));
        this.refreshPreFilteredList();
        this.updateCounts();
    }

    @Override
    public String getName()
    {
        return this.schematic.getMetadata().getName();
    }

    @Override
    public String getTitle()
    {
        return StringUtils.translate("litematica.title.screen.material_list.schematic",
                                     this.getName(), this.regions.size(), this.schematic.getRegionNames().size());
    }
}
