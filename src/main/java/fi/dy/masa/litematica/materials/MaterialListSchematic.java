package fi.dy.masa.litematica.materials;

import java.util.Collection;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.util.StringUtils;

public class MaterialListSchematic extends MaterialListBase
{
    private final LitematicaSchematic schematic;
    private final ImmutableList<String> regions;

    public MaterialListSchematic(LitematicaSchematic schematic, boolean reCreate)
    {
        this(schematic, schematic.getAreas().keySet(), reCreate);
    }

    public MaterialListSchematic(LitematicaSchematic schematic, Collection<String> subRegions, boolean reCreate)
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
        return StringUtils.translate("litematica.gui.title.material_list.schematic", this.getName(), this.regions.size(), this.schematic.getAreas().size());
    }
}
