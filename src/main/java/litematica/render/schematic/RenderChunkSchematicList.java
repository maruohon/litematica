package litematica.render.schematic;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

public class RenderChunkSchematicList extends RenderChunkSchematicVbo
{
    private static final int BLOCK_LAYERS = BlockRenderLayer.values().length;
    private static final int LIST_SIZE = BLOCK_LAYERS + OverlayRenderType.values().length;

    private final int baseDisplayList;
    private final int baseOverlay;

    public RenderChunkSchematicList(World worldIn, RenderGlobal renderGlobalIn, int index)
    {
        super(worldIn, renderGlobalIn, index);

        this.baseDisplayList = GLAllocation.generateDisplayLists(LIST_SIZE);
        this.baseOverlay = this.baseDisplayList + BLOCK_LAYERS;
    }

    public int getDisplayList(BlockRenderLayer layer, CompiledChunk compiledChunk)
    {
        return compiledChunk.isLayerEmpty(layer) == false ? this.baseDisplayList + layer.ordinal() : -1;
    }

    public int getOverlayDisplayList(OverlayRenderType type, CompiledChunkSchematic compiledChunk)
    {
        return compiledChunk.isOverlayTypeEmpty(type) == false ? this.baseOverlay + type.ordinal() : -1;
    }

    public void deleteGlResources()
    {
        super.deleteGlResources();

        GLAllocation.deleteDisplayLists(this.baseDisplayList, LIST_SIZE);
    }
}
