package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.BlockRenderLayer;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final BufferBuilder[] worldRenderers;
    private final BufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        this.worldRenderers = new BufferBuilder[BlockRenderLayer.values().length];
        this.overlayBufferBuilders = new BufferBuilder[OverlayRenderType.values().length];

        this.worldRenderers[BlockRenderLayer.SOLID.ordinal()] = new BufferBuilder(2097152);
        this.worldRenderers[BlockRenderLayer.CUTOUT.ordinal()] = new BufferBuilder(131072);
        this.worldRenderers[BlockRenderLayer.CUTOUT_MIPPED.ordinal()] = new BufferBuilder(131072);
        this.worldRenderers[BlockRenderLayer.TRANSLUCENT.ordinal()] = new BufferBuilder(262144);

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    public BufferBuilder getWorldRendererByLayer(BlockRenderLayer layer)
    {
        return this.worldRenderers[layer.ordinal()];
    }

    public BufferBuilder getWorldRendererByLayerId(int id)
    {
        return this.worldRenderers[id];
    }

    public BufferBuilder getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }
}
