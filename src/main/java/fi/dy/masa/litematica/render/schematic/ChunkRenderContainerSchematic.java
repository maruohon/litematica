package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.ChunkRenderContainer;

public abstract class ChunkRenderContainerSchematic extends ChunkRenderContainer
{
    protected List<RenderChunkSchematicVbo> overlayRenderChunks = new ArrayList<>(17424);

    public void addOverlayChunk(RenderChunkSchematicVbo renderChunk)
    {
        this.overlayRenderChunks.add(renderChunk);
    }

    public abstract void renderBlockOverlays();
}
