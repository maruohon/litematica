package litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.ChunkRenderContainer;

import litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;

public abstract class ChunkRenderContainerSchematic extends ChunkRenderContainer
{
    protected List<RenderChunkSchematicVbo> overlayRenderChunks = new ArrayList<>(128);

    @Override
    public void initialize(double viewEntityXIn, double viewEntityYIn, double viewEntityZIn)
    {
        super.initialize(viewEntityXIn, viewEntityYIn, viewEntityZIn);

        this.overlayRenderChunks.clear();
    }

    public void addOverlayChunk(RenderChunkSchematicVbo renderChunk)
    {
        this.overlayRenderChunks.add(renderChunk);
    }

    public abstract void renderBlockOverlays(OverlayRenderType type);
}
