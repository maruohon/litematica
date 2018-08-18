package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

public class RenderChunkSchematicList extends RenderChunkSchematicVbo
{
    private static final int LIST_SIZE = BlockRenderLayer.values().length + 2;

    private final int baseDisplayList = GLAllocation.generateDisplayLists(LIST_SIZE);

    public RenderChunkSchematicList(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);
    }

    public int getDisplayList(BlockRenderLayer layer, CompiledChunk compiledChunk)
    {
        return compiledChunk.isLayerEmpty(layer) == false ? this.baseDisplayList + layer.ordinal() : -1;
    }

    public int getOverlayDisplayList(boolean outlineBuffer)
    {
        if (this.hasOverlay() == false)
        {
            return -1;
        }

        return this.baseDisplayList + (outlineBuffer ? LIST_SIZE - 2 : LIST_SIZE - 1);
    }

    public void deleteGlResources()
    {
        super.deleteGlResources();

        GLAllocation.deleteDisplayLists(this.baseDisplayList, LIST_SIZE);
    }
}
