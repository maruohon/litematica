package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

public class RenderChunkSchematicList extends RenderChunkSchematicVbo
{
    private final int baseDisplayList = GLAllocation.generateDisplayLists(BlockRenderLayer.values().length);

    public RenderChunkSchematicList(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);
    }

    public int getDisplayList(BlockRenderLayer layer, CompiledChunk compiledChunk)
    {
        return compiledChunk.isLayerEmpty(layer) == false ? this.baseDisplayList + layer.ordinal() : -1;
    }

    public void deleteGlResources()
    {
        super.deleteGlResources();
        GLAllocation.deleteDisplayLists(this.baseDisplayList, BlockRenderLayer.values().length);
    }
}
