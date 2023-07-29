package litematica.render.schematic;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.buffer.VanillaWrappingVertexBuilder;
import malilib.render.buffer.VertexBuilder;
import litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class VertexBuilderCache
{
    private final VertexBuilder[] worldRenderers;
    private final VertexBuilder[] overlayBufferBuilders;

    public VertexBuilderCache()
    {
        this.worldRenderers = new VertexBuilder[BlockRenderLayer.values().length];
        this.overlayBufferBuilders = new VertexBuilder[2];

        this.worldRenderers[BlockRenderLayer.SOLID.ordinal()] = VanillaWrappingVertexBuilder.create(2097152, GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        this.worldRenderers[BlockRenderLayer.CUTOUT.ordinal()] = VanillaWrappingVertexBuilder.create(131072, GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        this.worldRenderers[BlockRenderLayer.CUTOUT_MIPPED.ordinal()] = VanillaWrappingVertexBuilder.create(131072, GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        this.worldRenderers[BlockRenderLayer.TRANSLUCENT.ordinal()] = VanillaWrappingVertexBuilder.create(262144, GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

        this.overlayBufferBuilders[0] = VanillaWrappingVertexBuilder.create(262144, GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        this.overlayBufferBuilders[1] = VanillaWrappingVertexBuilder.create(262144, GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    }

    public VertexBuilder getWorldRendererByLayer(BlockRenderLayer layer)
    {
        return this.worldRenderers[layer.ordinal()];
    }

    public VertexBuilder getWorldRendererByLayerId(int id)
    {
        return this.worldRenderers[id];
    }

    public VertexBuilder getOverlayBuffer(OverlayRenderType type)
    {
        return this.overlayBufferBuilders[type.ordinal()];
    }
}
