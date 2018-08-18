package fi.dy.masa.litematica.render.schematic;

import java.util.EnumSet;
import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;

public class RenderListSchematic extends ChunkRenderContainerSchematic
{
    @Override
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized)
        {
            for (RenderChunk renderChunk : this.renderChunks)
            {
                ListedRenderChunk listedrenderchunk = (ListedRenderChunk) renderChunk;
                GlStateManager.pushMatrix();
                this.preRenderChunk(renderChunk);
                GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getCompiledChunk()));
                GlStateManager.popMatrix();
            }

            GlStateManager.resetColor();
            this.renderChunks.clear();
        }
    }

    @Override
    public void renderBlockOverlays()
    {
        if (this.initialized)
        {
            for (RenderChunkSchematicVbo renderChunk : this.overlayRenderChunks)
            {
                RenderChunkSchematicList listedRenderChunk = (RenderChunkSchematicList) renderChunk;

                EnumSet<OverlayType> types = listedRenderChunk.getOverlayTypes();

                if (types.isEmpty() == false)
                {
                    for (OverlayType type : types)
                    {
                        GlStateManager.pushMatrix();
                        this.preRenderChunk(renderChunk);
                        GlStateManager.callList(listedRenderChunk.getOverlayDisplayList(type));
                        GlStateManager.popMatrix();
                    }
                }
            }

            GlStateManager.resetColor();
            this.overlayRenderChunks.clear();
        }
    }
}
