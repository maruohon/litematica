package fi.dy.masa.litematica.render.schematic;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.client.gl.GlBuffer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;

public class ChunkRendererListSchematicVbo extends ChunkRendererListSchematicBase
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.isCameraPositionSet)
        {
            for (ChunkRendererSchematicVbo renderChunk : this.chunkRenderers)
            {
                this.renderBlocks(renderChunk.getBlocksGlBufferByLayer(layer), renderChunk);
            }

            GlBuffer.unbind();
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

            GlBuffer.unbind();
            GlStateManager.clearCurrentColor();

            this.overlayChunkRenderers.clear();
        }
    }

    private void renderBlocks(GlBuffer vertexBuffer, ChunkRendererSchematicVbo renderChunk)
    {
        GlStateManager.pushMatrix();

        this.preRenderChunk(renderChunk);
        //renderChunk.multModelviewMatrix();
        vertexBuffer.bind();
        this.setupArrayPointersBlocks();
        vertexBuffer.draw(GL11.GL_QUADS);

        GlStateManager.popMatrix();
    }

    private void renderOverlay(GlBuffer vertexBuffer, ChunkRendererSchematicVbo renderChunk, int glMode)
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
        RenderSystem.glClientActiveTexture(GL13.GL_TEXTURE1);
        GlStateManager.texCoordPointer(2, GL11.GL_SHORT, 28, 24);
        RenderSystem.glClientActiveTexture(GL13.GL_TEXTURE0);
    }

    private void setupArrayPointersOverlay()
    {
        GlStateManager.vertexPointer(3, GL11.GL_FLOAT, 16, 0);
        GlStateManager.colorPointer(4, GL11.GL_UNSIGNED_BYTE, 16, 12);
    }
}
