package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayRenderType;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;

public class CompiledChunkSchematic extends CompiledChunk
{
    private final boolean[] overlayLayersUsed = new boolean[OverlayRenderType.values().length];
    private final boolean[] overlayLayersStarted = new boolean[OverlayRenderType.values().length];
    private final BufferBuilder.State[] blockBufferStates = new BufferBuilder.State[BlockRenderLayer.values().length];
    private final BufferBuilder.State[] overlayBufferStates = new BufferBuilder.State[OverlayRenderType.values().length];
    private boolean overlayEmpty = true;
    private long timeBuilt;

    @Override
    public void setLayerUsed(BlockRenderLayer layer)
    {
        super.setLayerUsed(layer);
    }

    public boolean isOverlayEmpty()
    {
        return this.overlayEmpty;
    }

    protected void setOverlayTypeUsed(OverlayRenderType type)
    {
        this.overlayEmpty = false;
        this.overlayLayersUsed[type.ordinal()] = true;
    }

    public boolean isOverlayTypeEmpty(OverlayRenderType type)
    {
        return this.overlayLayersUsed[type.ordinal()] == false;
    }

    public void setOverlayTypeStarted(OverlayRenderType type)
    {
        this.overlayLayersStarted[type.ordinal()] = true;
    }

    public boolean isOverlayTypeStarted(OverlayRenderType type)
    {
        return this.overlayLayersStarted[type.ordinal()];
    }

    public BufferBuilder.State getBlockBufferState(BlockRenderLayer layer)
    {
        return this.blockBufferStates[layer.ordinal()];
    }

    public void setBlockBufferState(BlockRenderLayer layer, BufferBuilder.State state)
    {
        this.blockBufferStates[layer.ordinal()] = state;
    }

    public BufferBuilder.State getOverlayBufferState(OverlayRenderType type)
    {
        return this.overlayBufferStates[type.ordinal()];
    }

    public void setOverlayBufferState(OverlayRenderType type, BufferBuilder.State state)
    {
        this.overlayBufferStates[type.ordinal()] = state;
    }

    public long getTimeBuilt()
    {
        return this.timeBuilt;
    }

    public void setTimeBuilt(long time)
    {
        this.timeBuilt = time;
    }
}
