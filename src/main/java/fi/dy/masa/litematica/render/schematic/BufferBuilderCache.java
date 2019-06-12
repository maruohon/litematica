package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilder;

public class BufferBuilderCache
{
    private final BufferBuilder[] blockBufferBuilders = new BufferBuilder[BlockRenderLayer.values().length];
    private BufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        this.blockBufferBuilders[BlockRenderLayer.SOLID.ordinal()] = new BufferBuilder(2097152);
        this.blockBufferBuilders[BlockRenderLayer.CUTOUT.ordinal()] = new BufferBuilder(131072);
        this.blockBufferBuilders[BlockRenderLayer.MIPPED_CUTOUT.ordinal()] = new BufferBuilder(131072);
        this.blockBufferBuilders[BlockRenderLayer.TRANSLUCENT.ordinal()] = new BufferBuilder(262144);

        this.overlayBufferBuilders = new BufferBuilder[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    public BufferBuilder getBlockBufferByLayer(BlockRenderLayer layer)
    {
        return this.blockBufferBuilders[layer.ordinal()];
    }

    public BufferBuilder getBlockBufferByLayerId(int id)
    {
        return this.blockBufferBuilders[id];
    }

    public BufferBuilder getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }
}
