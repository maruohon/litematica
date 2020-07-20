package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.world.WorldSchematic;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.GlAllocationUtils;

public class ChunkRendererSchematicDisplaylist extends ChunkRendererSchematicVbo
{
    private static final int BLOCK_LAYERS = RenderLayer.values().length;
    private static final int LIST_SIZE = BLOCK_LAYERS + OverlayRenderType.values().length;

    private final int baseDisplayList;
    private final int baseOverlay;

    public ChunkRendererSchematicDisplaylist(WorldSchematic worldIn, WorldRendererSchematic worldRenderer)
    {
        super(worldIn, worldRenderer);

        this.baseDisplayList = GlAllocationUtils.genLists(LIST_SIZE);
        this.baseOverlay = this.baseDisplayList + BLOCK_LAYERS;
    }

    public int getDisplayList(RenderLayer layer, ChunkRenderDataSchematic data)
    {
        return data.isBlockLayerEmpty(layer) == false ? this.baseDisplayList + layer.ordinal() : -1;
    }

    public int getOverlayDisplayList(OverlayRenderType type, ChunkRenderDataSchematic compiledChunk)
    {
        return compiledChunk.isOverlayTypeEmpty(type) == false ? this.baseOverlay + type.ordinal() : -1;
    }

    public void deleteGlResources()
    {
        super.deleteGlResources();

        GlAllocationUtils.deleteLists(this.baseDisplayList, LIST_SIZE);
    }
}
