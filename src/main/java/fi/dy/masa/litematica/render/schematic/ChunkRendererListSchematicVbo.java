package fi.dy.masa.litematica.render.schematic;

import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;

public class ChunkRendererListSchematicVbo extends ChunkRendererListSchematicBase
{
    @Override
    public void renderChunkLayer(RenderLayer layer)
    {
        if (this.isCameraPositionSet)
        {
            for (ChunkRendererSchematicVbo renderChunk : this.chunkRenderers)
            {
                this.renderBlocks(renderChunk.getBlocksGlBufferByLayer(layer), renderChunk);
            }

            VertexBuffer.unbind();
            GlStateManager.clearCurrentColor();

            this.chunkRenderers.clear();
        }
    }

    @Override
    public void renderBlockOverlays(OverlayRenderType type)
    {
        if (this.isCameraPositionSet)
        {
            for (ChunkRendererSchematicVbo renderChunk : this.overlayChunkRenderers)
            {
                this.renderOverlay(renderChunk.getOverlayGlBuffer(type), renderChunk, type.getGlMode());
            }

            VertexBuffer.unbind();
            GlStateManager.clearCurrentColor();

            this.overlayChunkRenderers.clear();
        }
    }

    private void renderBlocks(VertexBuffer vertexBuffer, ChunkRendererSchematicVbo renderChunk)
    {
        GlStateManager.pushMatrix();

        this.preRenderChunk(renderChunk);
        //renderChunk.multModelviewMatrix();
        vertexBuffer.bind();
        this.setupArrayPointersBlocks();
        vertexBuffer.draw(GL11.GL_QUADS);

        GlStateManager.popMatrix();
    }

    private void renderOverlay(VertexBuffer vertexBuffer, ChunkRendererSchematicVbo renderChunk, int glMode)
    {
        GlStateManager.pushMatrix();

        this.preRenderChunk(renderChunk);
        //renderChunk.multModelviewMatrix();
        vertexBuffer.bind();
        this.setupArrayPointersOverlay();
        vertexBuffer.draw(glMode);

        GlStateManager.popMatrix();
    }

    private void setupArrayPointersBlocks()
    {
        GlStateManager.vertexPointer(3, GL11.GL_FLOAT, 28, 0);
        GlStateManager.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
        GlStateManager.texCoordPointer(2, GL11.GL_FLOAT, 28, 16);
        GLX.glClientActiveTexture(GLX.GL_TEXTURE1);
        GlStateManager.texCoordPointer(2, GL11.GL_SHORT, 28, 24);
        GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
    }

    private void setupArrayPointersOverlay()
    {
        GlStateManager.vertexPointer(3, GL11.GL_FLOAT, 16, 0);
        GlStateManager.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 16, 12);
    }
}
