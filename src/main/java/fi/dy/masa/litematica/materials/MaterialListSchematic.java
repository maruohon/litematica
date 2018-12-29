package fi.dy.masa.litematica.materials;

import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.client.resources.I18n;

public class MaterialListSchematic extends MaterialListBase
{
    private final LitematicaSchematic schematic;
    private final ImmutableList<String> regions;

    public MaterialListSchematic(LitematicaSchematic schematic)
    {
        this(schematic, schematic.getAreas().keySet());
    }

    public MaterialListSchematic(LitematicaSchematic schematic, Collection<String> subRegions)
    {
        super();

        this.schematic = schematic;
        this.regions = ImmutableList.copyOf(subRegions);
    }

    @Override
    public String getName()
    {
        return this.schematic.getMetadata().getName();
    }

    @Override
    public String getTitle()
    {
        return I18n.format("litematica.gui.title.material_list.schematic", this.getName(), this.regions.size(), this.schematic.getAreas().size());
    }

    @Override
    protected List<MaterialListEntry> createMaterialListEntries()
    {
        return MaterialListUtils.createMaterialListFor(this.schematic, this.regions);
    }
}
