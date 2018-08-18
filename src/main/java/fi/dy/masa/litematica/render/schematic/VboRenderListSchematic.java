package fi.dy.masa.litematica.render.schematic;

import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;

public class VboRenderListSchematic extends ChunkRenderContainerSchematic
{
    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn)
    {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);

        this.overlayRenderChunks.clear();
    }

    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized)
        {
            for (RenderChunk renderChunk : this.renderChunks)
            {
                this.renderBlocks(renderChunk.getVertexBufferByLayer(layer.ordinal()), renderChunk);
            }

            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.resetColor();

            this.renderChunks.clear();
        }
    }

    @Override
    public void renderBlockOverlays()
    {
        if (this.initialized)
        {
            GlStateManager.glLineWidth(1.0f);

            if (GuiScreen.isCtrlKeyDown()) System.out.printf("VboRenderListSchematic.renderBlockOverlays()\n");
            for (RenderChunkSchematicVbo renderChunk : this.overlayRenderChunks)
            {
                this.renderOverlay(renderChunk.getOverlayVertexBuffer(true), renderChunk, GL11.GL_QUADS);
                this.renderOverlay(renderChunk.getOverlayVertexBuffer(false), renderChunk, GL11.GL_QUADS);
            }

            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.resetColor();

            this.overlayRenderChunks.clear();
        }
    }

    private void renderBlocks(VertexBuffer vertexBuffer, RenderChunk renderChunk)
    {
        GlStateManager.pushMatrix();
        this.preRenderChunk(renderChunk);
        renderChunk.multModelviewMatrix();
        vertexBuffer.bindBuffer();
        this.setupArrayPointersBlocks();
        vertexBuffer.drawArrays(GL11.GL_QUADS);
        GlStateManager.popMatrix();
    }

    private void renderOverlay(VertexBuffer vertexBuffer, RenderChunk renderChunk, int mode)
    {
        GlStateManager.pushMatrix();
        this.preRenderChunk(renderChunk);
        renderChunk.multModelviewMatrix();
        vertexBuffer.bindBuffer();
        this.setupArrayPointersOverlay();
        vertexBuffer.drawArrays(mode);
        GlStateManager.popMatrix();
    }

    private void setupArrayPointersBlocks()
    {
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 28, 0);
        GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 28, 12);
        GlStateManager.glTexCoordPointer(2, GL11.GL_FLOAT, 28, 16);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexCoordPointer(2, GL11.GL_SHORT, 28, 24);
        OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void setupArrayPointersOverlay()
    {
        GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 16, 0);
        GlStateManager.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, 16, 12);
    }
}
