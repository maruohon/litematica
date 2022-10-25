package litematica.schematic.container;

import net.minecraft.block.state.IBlockState;

public class VanillaStructurePalette extends LitematicaBlockStatePaletteHashMap
{
    public VanillaStructurePalette()
    {
        super(32, null);
    }

    @Override
    public int idFor(IBlockState state)
    {
        int id = this.statePaletteMap.getId(state);

        if (id == -1)
        {
            id = this.statePaletteMap.add(state);
        }

        return id;
    }

    @Override
    public VanillaStructurePalette copy(IPaletteResizeHandler resizeHandler)
    {
        VanillaStructurePalette copy = new VanillaStructurePalette();

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            copy.statePaletteMap.add(this.statePaletteMap.get(id));
        }

        return copy;
    }
}
