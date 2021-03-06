package fi.dy.masa.litematica.render.schematic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class ChunkRenderDataSchematic
{
    public static final ChunkRenderDataSchematic EMPTY = new ChunkRenderDataSchematic() {
        @Override
        public void setBlockLayerUsed(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setBlockLayerStarted(RenderLayer layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverlayTypeUsed(OverlayRenderType layer)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOverlayTypeStarted(OverlayRenderType layer)
        {
            throw new UnsupportedOperationException();
        }
    };

    private final Set<RenderLayer> blockLayersUsed = new ObjectArraySet<>();
    private final Set<RenderLayer> blockLayersStarted = new ObjectArraySet<>();
    private final List<BlockEntity> blockEntities = new ArrayList<>();

    private final boolean[] overlayLayersUsed = new boolean[OverlayRenderType.values().length];
    private final boolean[] overlayLayersStarted = new boolean[OverlayRenderType.values().length];
    private final Map<RenderLayer, BufferBuilder.State> blockBufferStates = new HashMap<>();
    private final BufferBuilder.State[] overlayBufferStates = new BufferBuilder.State[OverlayRenderType.values().length];
    private boolean overlayEmpty = true;
    private boolean empty = true;
    private long timeBuilt;

    public boolean isEmpty()
    {
        return this.empty;
    }

    public boolean isBlockLayerEmpty(RenderLayer layer)
    {
        return ! this.blockLayersUsed.contains(layer);
    }

    public void setBlockLayerUsed(RenderLayer layer)
    {
        this.blockLayersUsed.add(layer);
        this.empty = false;
    }

    public boolean isBlockLayerStarted(RenderLayer layer)
    {
        return this.blockLayersStarted.contains(layer);
    }

    public void setBlockLayerStarted(RenderLayer layer)
    {
        this.blockLayersStarted.add(layer);
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
        return ! this.overlayLayersUsed[type.ordinal()];
    }

    public void setOverlayTypeStarted(OverlayRenderType type)
    {
        this.overlayLayersStarted[type.ordinal()] = true;
    }

    public boolean isOverlayTypeStarted(OverlayRenderType type)
    {
        return this.overlayLayersStarted[type.ordinal()];
    }

    public BufferBuilder.State getBlockBufferState(RenderLayer layer)
    {
        return this.blockBufferStates.get(layer);
    }

    public void setBlockBufferState(RenderLayer layer, BufferBuilder.State state)
    {
        this.blockBufferStates.put(layer, state);
    }

    public BufferBuilder.State getOverlayBufferState(OverlayRenderType type)
    {
        return this.overlayBufferStates[type.ordinal()];
    }

    public void setOverlayBufferState(OverlayRenderType type, BufferBuilder.State state)
    {
        this.overlayBufferStates[type.ordinal()] = state;
    }

    public List<BlockEntity> getBlockEntities()
    {
        return this.blockEntities;
    }

    public void addBlockEntity(BlockEntity be)
    {
        this.blockEntities.add(be);
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
