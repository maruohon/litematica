package litematica.render.schematic;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.RenderContext;
import malilib.util.game.wrap.RenderWrap;
import litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class VboRenderListSchematic extends ChunkRenderContainerSchematic
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized)
        {
            for (RenderChunk renderChunk : this.renderChunks)
            {
                this.renderBlocks(renderChunk.getVertexBufferByLayer(layer.ordinal()), renderChunk, RenderContext.DUMMY);
            }

            RenderWrap.bindBuffer(RenderWrap.GL_ARRAY_BUFFER, 0);
            RenderWrap.resetColor();

            this.renderChunks.clear();
        }
    }

    @Override
    public void renderBlockOverlays(OverlayRenderType type)
    {
        if (this.initialized)
        {
            for (RenderChunkSchematicVbo renderChunk : this.overlayRenderChunks)
            {
                this.renderOverlay(renderChunk.getOverlayVertexBuffer(type), renderChunk, type.getGlMode(), RenderContext.DUMMY);
            }

            RenderWrap.bindBuffer(RenderWrap.GL_ARRAY_BUFFER, 0);
            RenderWrap.resetColor();

            this.overlayRenderChunks.clear();
        }
    }

    private void renderBlocks(VertexBuffer vertexBuffer, RenderChunk renderChunk, RenderContext ctx)
    {
        RenderWrap.pushMatrix(ctx);

        this.preRenderChunk(renderChunk);
        //renderChunk.multModelviewMatrix();
        vertexBuffer.bindBuffer();
        this.setupArrayPointersBlocks();
        vertexBuffer.drawArrays(GL11.GL_QUADS);

        RenderWrap.popMatrix(ctx);
    }

    private void renderOverlay(VertexBuffer vertexBuffer, RenderChunk renderChunk, int glMode, RenderContext ctx)
    {
        RenderWrap.pushMatrix(ctx);

        this.preRenderChunk(renderChunk);
        //renderChunk.multModelviewMatrix();
        vertexBuffer.bindBuffer();
        this.setupArrayPointersOverlay();
        vertexBuffer.drawArrays(glMode);

        RenderWrap.popMatrix(ctx);
    }

    private void setupArrayPointersBlocks()
    {
        RenderWrap.vertexPointer(3, GL11.GL_FLOAT, 28, 0);
        RenderWrap.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
        RenderWrap.texCoordPointer(2, GL11.GL_FLOAT, 28, 16);
        RenderWrap.setClientActiveTexture(RenderWrap.LIGHTMAP_TEX_UNIT);
        RenderWrap.texCoordPointer(2, GL11.GL_SHORT, 28, 24);
        RenderWrap.setClientActiveTexture(RenderWrap.DEFAULT_TEX_UNIT);
    }

    private void setupArrayPointersOverlay()
    {
        RenderWrap.vertexPointer(3, GL11.GL_FLOAT, 16, 0);
        RenderWrap.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 16, 12);
    }
}
