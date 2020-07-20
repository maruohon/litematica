package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;

public class BufferBuilderCache
{
    private final BufferBuilder[] blockBufferBuilders = new BufferBuilder[RenderLayer.values().length];
    private BufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        this.blockBufferBuilders[RenderLayer.SOLID.ordinal()] = new BufferBuilder(2097152);
        this.blockBufferBuilders[RenderLayer.CUTOUT.ordinal()] = new BufferBuilder(131072);
        this.blockBufferBuilders[RenderLayer.CUTOUT_MIPPED.ordinal()] = new BufferBuilder(131072);
        this.blockBufferBuilders[RenderLayer.TRANSLUCENT.ordinal()] = new BufferBuilder(262144);

        this.overlayBufferBuilders = new BufferBuilder[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    public BufferBuilder getBlockBufferByLayer(RenderLayer layer)
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
