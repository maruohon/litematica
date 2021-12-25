package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PositionUtils.ChunkPosComparator;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public abstract class TaskPasteSchematicPerChunkBase extends TaskBase implements IInfoHudRenderer
{
    protected final ImmutableList<SchematicPlacement> placements;
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final List<ChunkPos> pendingChunks = new ArrayList<>();
    protected final ChunkPosComparator comparator;
    protected final WorldSchematic schematicWorld;
    protected final LayerRange layerRange;
    protected final ReplaceBehavior replace;
    protected final boolean changedBlockOnly;
    private final HashSet<ChunkPos> allChunksSet = new HashSet<>();

    public TaskPasteSchematicPerChunkBase(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        this.placements = ImmutableList.copyOf(placements);
        this.layerRange = range;
        this.changedBlockOnly = changedBlocksOnly;
        this.comparator = new ChunkPosComparator();
        this.comparator.setClosestFirst(true);
        this.replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();
        this.name = StringUtils.translate("litematica.gui.label.task_name.paste");
        this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
    }

    @Override
    public void init()
    {
        for (SchematicPlacement placement : this.placements)
        {
            this.addPlacement(placement, this.layerRange);
        }

        this.pendingChunks.addAll(this.allChunksSet);
        this.sortChunkList();

        InfoHud.getInstance().addInfoHudRenderer(this, true);
        this.updateInfoHudLines();
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() &&
               this.mc.player != null &&
               this.schematicWorld != null &&
               this.pendingChunks.isEmpty() == false;
    }

    protected void addPlacement(SchematicPlacement placement, LayerRange range)
    {
        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();

        for (ChunkPos pos : touchedChunks)
        {
            int count = 0;

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
            {
                box = this.getClampedBox(box, range);

                if (box != null)
                {
                    this.boxesInChunks.put(pos, box);
                    ++count;
                }
            }

            if (count > 0)
            {
                this.allChunksSet.add(pos);
                this.onChunkAddedForHandling(pos, placement);
            }
        }
    }

    /**
     * Clamps the given box to the layer range bounds.
     * @param box
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public IntBoundingBox getClampedBox(IntBoundingBox box, LayerRange range)
    {
        return this.getClampedArea(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, range);
    }

    /**
     * Clamps the given box to the layer range bounds.
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public IntBoundingBox getClampedArea(BlockPos posMin, BlockPos posMax, LayerRange range)
    {
        return this.getClampedArea(posMin.getX(), posMin.getY(), posMin.getZ(),
                                   posMax.getX(), posMax.getY(), posMax.getZ(), range);
    }

    /**
     * Clamps the given box to the layer range bounds.
     * @return the clamped box, or null, if the range does not intersect the original box
     */
    @Nullable
    public IntBoundingBox getClampedArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LayerRange range)
    {
        if (range.intersectsBox(minX, minY, minZ, maxX, maxY, maxZ) == false)
        {
            return null;
        }

        switch (range.getAxis())
        {
            case X:
            {
                final int clampedMinX = Math.max(minX, range.getLayerMin());
                final int clampedMaxX = Math.min(maxX, range.getLayerMax());
                return IntBoundingBox.createProper(clampedMinX, minY, minZ, clampedMaxX, maxY, maxZ);
            }
            case Y:
            {
                final int clampedMinY = Math.max(minY, range.getLayerMin());
                final int clampedMaxY = Math.min(maxY, range.getLayerMax());
                return IntBoundingBox.createProper(minX, clampedMinY, minZ, maxX, clampedMaxY, maxZ);
            }
            case Z:
            {
                final int clampedMinZ = Math.max(minZ, range.getLayerMin());
                final int clampedMaxZ = Math.min(maxZ, range.getLayerMax());
                return IntBoundingBox.createProper(minX, minY, clampedMinZ, maxX, maxY, clampedMaxZ);
            }
            default:
                return null;
        }
    }

    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
    {
    }

    protected boolean canProcessChunk(ChunkPos pos, WorldSchematic worldSchematic, ClientWorld worldClient)
    {
        if (worldSchematic.getChunkProvider().isChunkLoaded(pos.x, pos.z) == false ||
            DataManager.getSchematicPlacementManager().hasPendingRebuildFor(pos))
        {
            return false;
        }

        // Chunk exists in the schematic world, and all the surrounding chunks are loaded in the client world, good to go
        return this.areSurroundingChunksLoaded(pos, worldClient, 1);
    }

    protected void sortChunkList()
    {
        if (this.pendingChunks.size() > 0)
        {
            if (this.mc.player != null)
            {
                this.comparator.setReferencePosition(this.mc.player.getBlockPos());
                this.pendingChunks.sort(this.comparator);
            }

            this.onChunkListSorted();
        }
    }

    protected void onChunkListSorted()
    {
    }

    protected void updateInfoHudLines()
    {
        List<String> hudLines = new ArrayList<>();

        String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
        String title = StringUtils.translate("litematica.gui.label.schematic_paste.missing_chunks", this.pendingChunks.size());
        hudLines.add(String.format("%s%s%s", pre, title, GuiBase.TXT_RST));

        int maxLines = Math.min(this.pendingChunks.size(), Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue());

        for (int i = 0; i < maxLines; ++i)
        {
            ChunkPos pos = this.pendingChunks.get(i);
            hudLines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x, pos.z, pos.x << 4, pos.z << 4));
        }

        this.infoHudLines = hudLines;
    }
}
