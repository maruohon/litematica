package fi.dy.masa.litematica.render.schematic;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class BufferBuilderCache
{
    private final Map<RenderType, BufferBuilder> blockBufferBuilders = new HashMap<>();
    private BufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache()
    {
        for (RenderType layer : RenderType.getBlockLayers())
        {
            this.blockBufferBuilders.put(layer, new BufferBuilder(layer.getExpectedBufferSize()));
        }

        this.overlayBufferBuilders = new BufferBuilder[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i)
        {
            this.overlayBufferBuilders[i] = new BufferBuilder(262144);
        }
    }

    public BufferBuilder getBlockBufferByLayer(RenderType layer)
    {
        return this.blockBufferBuilders.get(layer);
    }

    public BufferBuilder getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }

    public void clear()
    {
        this.blockBufferBuilders.values().forEach(BufferBuilder::reset);

        for (BufferBuilder buffer : this.overlayBufferBuilders)
        {
            buffer.reset();
        }
    }
}
