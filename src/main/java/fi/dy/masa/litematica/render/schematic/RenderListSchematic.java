package fi.dy.masa.litematica.render.schematic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class RenderListSchematic extends ChunkRenderContainerSchematic
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized)
        {
            for (RenderChunk renderChunk : this.renderChunks)
            {
                RenderChunkSchematicList listedrenderchunk = (RenderChunkSchematicList) renderChunk;
                GlStateManager.pushMatrix();
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getChunkRenderData()));
                GlStateManager.popMatrix();
            }

            GlStateManager.resetColor();
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
                RenderChunkSchematicList listedRenderChunk = (RenderChunkSchematicList) renderChunk;

                GlStateManager.pushMatrix();
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedRenderChunk.getOverlayDisplayList(type, listedRenderChunk.getChunkRenderData()));
                GlStateManager.popMatrix();
            }

            GlStateManager.resetColor();
            this.overlayRenderChunks.clear();
        }
    }
}
