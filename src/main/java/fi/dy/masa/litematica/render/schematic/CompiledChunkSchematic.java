package fi.dy.masa.litematica.render.schematic;

import fi.dy.masa.litematica.render.schematic.RenderChunkSchematicVbo.OverlayType;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.BlockRenderLayer;

public class CompiledChunkSchematic extends CompiledChunk
{
    private final boolean[] overlayLayersUsed = new boolean[OverlayType.values().length];
    private final boolean[] overlayLayersStarted = new boolean[OverlayType.values().length];
    private final BufferBuilder.State[] blockBufferStates = new BufferBuilder.State[BlockRenderLayer.values().length];
    private final BufferBuilder.State[] overlayBufferStates = new BufferBuilder.State[OverlayType.values().length];
    private boolean overlayEmpty = true;

    public boolean isOverlayEmpty()
    {
        return this.overlayEmpty;
    }

    protected void setOverlayTypeUsed(OverlayType type)
    {
        this.overlayEmpty = false;
        this.overlayLayersUsed[type.ordinal()] = true;
    }

    public boolean isOverlayTypeEmpty(OverlayType type)
    {
        return this.overlayLayersUsed[type.ordinal()] == false;
    }

    public void setOverlayTypeStarted(OverlayType type)
    {
        this.overlayLayersStarted[type.ordinal()] = true;
    }

    public boolean isOverlayTypeStarted(OverlayType type)
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

    public BufferBuilder.State getOverlayBufferState(OverlayType type)
    {
        return this.overlayBufferStates[type.ordinal()];
    }

    public void setOverlayBufferState(OverlayType type, BufferBuilder.State state)
    {
        this.overlayBufferStates[type.ordinal()] = state;
    }
}
