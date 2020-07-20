package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.platform.GlStateManager;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import net.minecraft.client.render.RenderLayer;

public class ChunkRendererListSchematicDisplaylist extends ChunkRendererListSchematicBase
{
    @Override
    public void renderChunkLayer(RenderLayer layer)
    {
        if (this.isCameraPositionSet)
        {
            for (ChunkRendererSchematicVbo renderChunk : this.chunkRenderers)
            {
                ChunkRendererSchematicDisplaylist listedrenderchunk = (ChunkRendererSchematicDisplaylist) renderChunk;

                GlStateManager.pushMatrix();

                this.preRenderChunk(renderChunk);

                GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getChunkRenderData()));
                GlStateManager.popMatrix();
            }

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
                ChunkRendererSchematicDisplaylist listedRenderChunk = (ChunkRendererSchematicDisplaylist) renderChunk;

                GlStateManager.pushMatrix();
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedRenderChunk.getOverlayDisplayList(type, (ChunkRenderDataSchematic) listedRenderChunk.getChunkRenderData()));
                GlStateManager.popMatrix();
            }

            GlStateManager.clearCurrentColor();

            this.overlayChunkRenderers.clear();
        }
    }
}
