package fi.dy.masa.litematica.materials;

import java.util.List;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import net.minecraft.client.resources.I18n;

public class MaterialListSchematic extends MaterialListBase
{
    private final LitematicaSchematic schematic;

    public MaterialListSchematic(LitematicaSchematic schematic)
    {
        super();

        this.schematic = schematic;
    }

    @Override
    public String getName()
    {
        return this.schematic.getMetadata().getName();
    }

    @Override
    public String getTitle()
    {
        return I18n.format("litematica.gui.title.material_list.schematic", this.getName());
    }

    @Override
    protected List<MaterialListEntry> createMaterialListEntries()
    {
        return MaterialListUtils.createMaterialListFor(this.schematic);
    }
}
