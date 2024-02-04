package litematica.render.schematic;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;

import malilib.render.RenderContext;
import malilib.util.game.wrap.RenderWrap;
import litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public class RenderListSchematic extends ChunkRenderContainerSchematic
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized)
        {
            RenderContext ctx = RenderContext.DUMMY;

            for (RenderChunk renderChunk : this.renderChunks)
            {
                RenderChunkSchematicList listedrenderchunk = (RenderChunkSchematicList) renderChunk;
                RenderWrap.pushMatrix(ctx);
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getChunkRenderData()));
                RenderWrap.popMatrix(ctx);
            }

            RenderWrap.resetColor();
            this.renderChunks.clear();
        }
    }

    @Override
    public void renderBlockOverlays(OverlayRenderType type)
    {
        if (this.initialized)
        {
            RenderContext ctx = RenderContext.DUMMY;

            for (RenderChunkSchematicVbo renderChunk : this.overlayRenderChunks)
            {
                RenderChunkSchematicList listedRenderChunk = (RenderChunkSchematicList) renderChunk;

                RenderWrap.pushMatrix(ctx);
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedRenderChunk.getOverlayDisplayList(type, listedRenderChunk.getChunkRenderData()));
                RenderWrap.popMatrix(ctx);
            }

            RenderWrap.resetColor();
            this.overlayRenderChunks.clear();
        }
    }
}
