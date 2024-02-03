package litematica.schematic.verifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import malilib.listener.EventListener;
import malilib.listener.TaskCompletionListener;
import malilib.util.StringUtils;
import malilib.util.data.EnabledCondition;
import malilib.util.data.RunStatus;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.render.infohud.IInfoHudRenderer;
import litematica.render.infohud.RenderPhase;
import litematica.scheduler.TaskScheduler;
import litematica.schematic.placement.SchematicPlacement;
import litematica.selection.SelectionBox;
import litematica.task.SchematicVerifierTask;
import litematica.util.PositionUtils;
import litematica.util.value.BlockInfoListType;

public class SchematicVerifier implements IInfoHudRenderer
{
    public static final IBlockState AIR = Blocks.AIR.getDefaultState();

    protected final ArrayList<SchematicPlacement> placements = new ArrayList<>();
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final LongOpenHashSet completedChunks = new LongOpenHashSet();

    protected final Long2ObjectOpenHashMap<Object2ObjectOpenHashMap<BlockStatePair, IntArrayList>> resultsPerChunk = new Long2ObjectOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockStatePair> countsPerPair = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<VerifierResultType> countsPerType = new Object2IntOpenHashMap<>();

    protected final HashSet<VerifierResultType> selectedTypes = new HashSet<>();
    protected final HashSet<BlockStatePair> selectedPairs = new HashSet<>();
    protected final HashSet<BlockStatePair> calculatedSelectedPairs = new HashSet<>();
    protected final HashSet<BlockStatePair> ignoredPairs = new HashSet<>();
    protected final HashSet<VerifierResultType> visibleCategories = new HashSet<>();

    protected final ArrayList<BlockPairTypePosition> allSelectedPositions = new ArrayList<>();
    protected final ArrayList<BlockPairTypePosition> closestSelectedPositions = new ArrayList<>();

    protected final ArrayList<String> infoHudLines = new ArrayList<>();
    protected RunStatus status = RunStatus.STOPPED;
    protected BlockInfoListType verifierType = BlockInfoListType.ALL;
    protected String name = "?";
    @Nullable protected SchematicVerifierTask task;
    @Nullable protected TaskCompletionListener completionListener;
    @Nullable protected EventListener statusChangeListener;
    @Nullable BlockPos lastSortPosition;
    protected boolean autoRefresh;
    protected boolean countsDirty;
    protected boolean infoHudEnabled;
    protected boolean selectedClosestPositionsDirty;
    protected boolean selectedPairsDirty;
    protected boolean selectedPositionsDirty;

    public SchematicVerifier()
    {
        this.visibleCategories.addAll(VerifierResultType.INCORRECT_TYPES);
    }

    public SchematicVerifier(SchematicPlacement placement)
    {
        this();

        this.placements.add(placement);
        this.name = placement.getName();
    }

    public BlockInfoListType getVerifierType()
    {
        return this.verifierType;
    }

    public void setVerifierType(BlockInfoListType type)
    {
        this.verifierType = type;
    }

    public void setStatusChangeListener(@Nullable EventListener statusChangeListener)
    {
        this.statusChangeListener = statusChangeListener;
    }

    public Set<ChunkPos> getTouchedChunks()
    {
        return this.boxesInChunks.keySet();
    }

    public boolean hasPlacement(SchematicPlacement placement)
    {
        return this.placements.contains(placement);
    }

    public boolean removePlacement(SchematicPlacement placement)
    {
        // TODO how should this be handled exactly?

        if (this.placements.remove(placement))
        {
            this.stop();
        }

        return this.placements.isEmpty();
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean getAutoRefresh()
    {
        return this.autoRefresh;
    }

    public void toggleInfoHudEnabled()
    {
        this.infoHudEnabled = ! this.infoHudEnabled;
    }

    public void toggleAutoRefreshEnabled()
    {
        this.autoRefresh = ! this.autoRefresh;
    }

    public RunStatus getStatus()
    {
        return this.status;
    }

    public boolean hasData()
    {
        return this.resultsPerChunk.isEmpty() == false;
    }

    public boolean isCategoryVisible(VerifierResultType type)
    {
        return this.visibleCategories.contains(type);
    }

    public void toggleCategoryVisible(VerifierResultType type)
    {
        if (this.visibleCategories.contains(type))
        {
            this.visibleCategories.remove(type);
        }
        else
        {
            this.visibleCategories.add(type);
        }
    }

    public int getVisibleCategoriesCount()
    {
        return this.visibleCategories.size();
    }

    public boolean hasIgnoredPairs()
    {
        return this.ignoredPairs.isEmpty() == false;
    }

    public int getTotalPositionCountFor(VerifierResultType type)
    {
        this.updateCountsIfDirty();
        return this.countsPerType.getInt(type);
    }

    public void start(TaskCompletionListener completionListener)
    {
        if (this.status != RunStatus.RUNNING)
        {
            this.completionListener = completionListener;
            this.task = new SchematicVerifierTask(this);
            this.task.setCompletionListener(this::onTaskFinished);

            if (this.status != RunStatus.PAUSED)
            {
                this.clear();
                this.updateRequiredBoxes();
                this.task.setBoxes(this.boxesInChunks);
            }

            this.status = RunStatus.RUNNING;
            TaskScheduler.getInstanceClient().scheduleTask(this.task, 5);
        }
        else
        {
            this.pause();
        }
    }

    public void pause()
    {
        if (this.status == RunStatus.RUNNING)
        {
            this.stopTask();
            this.status = RunStatus.PAUSED;
        }
    }

    public void stop()
    {
        if (this.status != RunStatus.STOPPED)
        {
            this.stopTask();
            this.status = RunStatus.STOPPED;
        }
    }

    protected void onTaskFinished()
    {
        this.status = RunStatus.FINISHED;
        this.task = null;

        if (this.completionListener != null)
        {
            this.completionListener.onTaskCompleted();
        }
    }

    public void reset()
    {
        this.stopTask();
        this.clear();
    }

    protected void stopTask()
    {
        if (this.task != null)
        {
            this.task.stop();
            this.removeTask();
        }
    }

    protected void removeTask()
    {
        this.task = null;
    }

    public void resetIgnored()
    {
        this.ignoredPairs.clear();
    }

    public void clearSelection()
    {
        this.selectedPairs.clear();
        this.selectedTypes.clear();
        this.calculatedSelectedPairs.clear();
        this.selectedPositionsDirty = true;
    }

    public void ignoreStatePair(BlockStatePair pair)
    {
        this.ignoredPairs.add(pair);

        if (this.calculatedSelectedPairs.contains(pair))
        {
            this.selectedPairsDirty = true;
        }
    }

    public void toggleTypeSelected(VerifierResultType type)
    {
        if (this.selectedTypes.contains(type))
        {
            this.selectedTypes.remove(type);
        }
        else
        {
            this.selectedTypes.add(type);
        }

        this.selectedPairsDirty = true;
    }

    public void togglePairSelected(BlockStatePair pair)
    {
        if (this.selectedPairs.contains(pair))
        {
            this.selectedPairs.remove(pair);
        }
        else
        {
            this.selectedPairs.add(pair);
        }

        this.selectedPairsDirty = true;
    }

    public boolean isTypeSelected(VerifierResultType type)
    {
        return this.selectedTypes.contains(type);
    }

    public boolean isPairSelected(BlockStatePair pair)
    {
        return this.selectedPairs.contains(pair);
    }

    public VerifierStatus getCurrentStatus()
    {
        int processedChunks = this.completedChunks.size();
        int totalChunks = this.boxesInChunks.keySet().size();
        int correctBlocks = 0;
        int totalBlocks = 0;

        this.updateCountsIfDirty();

        for (BlockStatePair pair : this.countsPerPair.keySet())
        {
            if (pair.expectedState != AIR)
            {
                int count = this.countsPerPair.getInt(pair);
                totalBlocks += count;

                if (pair.type == VerifierResultType.CORRECT_STATE)
                {
                    correctBlocks += count;
                }
            }
        }

        return new VerifierStatus(this.status, processedChunks, totalChunks, totalBlocks, correctBlocks);
    }

    public void reCheckChunks(LongOpenHashSet reCheckChunks)
    {
        if (this.autoRefresh && this.status != RunStatus.STOPPED)
        {
            ArrayListMultimap<ChunkPos, IntBoundingBox> boxes = ArrayListMultimap.create();

            for (long posLong : reCheckChunks)
            {
                if (this.completedChunks.contains(posLong))
                {
                    ChunkPos pos = malilib.util.position.PositionUtils.chunkPosFromLong(posLong);
                    boxes.putAll(pos, this.boxesInChunks.get(pos));
                }
            }

            if (this.task == null)
            {
                this.task = new SchematicVerifierTask(this);
                this.task.setCompletionListener(this::removeTask);
                TaskScheduler.getInstanceClient().scheduleTask(this.task, 5);
            }

            this.task.replaceBoxes(boxes);
        }
    }

    public void addBlockResultsFromWorld(ChunkPos chunkPos, Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> results)
    {
        long chunkPosLong = ChunkPos.asLong(chunkPos.x, chunkPos.z);

        this.resultsPerChunk.put(chunkPosLong, results);

        /*
        Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> old = this.resultsPerChunk.get(chunkPosLong);

        if (old != null)
        {
            // Subtract the old counts
            old.forEach((pair, list) -> {
                int size = list.size();
                this.countsPerPair.addTo(pair, -size);
                this.countsPerType.addTo(pair.type, -size);
            });
        }


        // Add the new counts
        results.forEach((pair, list) -> {
            int size = list.size();
            this.countsPerPair.addTo(pair, size);
            this.countsPerType.addTo(pair.type, size);
        });
        */

        this.completedChunks.add(chunkPosLong);
        this.countsDirty = true;
        this.selectedPairsDirty = true;
        this.selectedPositionsDirty = true;

        if (this.statusChangeListener != null)
        {
            this.statusChangeListener.onEvent();
        }
    }

    public List<BlockStatePairCount> getNonIgnoredBlockPairs()
    {
        this.updateCountsIfDirty();
        ArrayList<BlockStatePairCount> list = new ArrayList<>();

        for (BlockStatePair pair : this.countsPerPair.keySet())
        {
            if (this.ignoredPairs.contains(pair) == false)
            {
                list.add(BlockStatePairCount.of(pair, this.countsPerPair.getInt(pair)));
            }
        }

        return list;
    }

    public List<BlockPairTypePosition> getClosestSelectedPositions(BlockPos referencePos)
    {
        this.updateClosestPositionsIfDirty(referencePos);
        return this.closestSelectedPositions;
    }

    protected void clear()
    {
        this.boxesInChunks.clear();
        this.completedChunks.clear();

        this.resultsPerChunk.clear();
        this.countsPerPair.clear();
        this.countsPerType.clear();

        this.selectedTypes.clear();
        this.selectedPairs.clear();
        this.calculatedSelectedPairs.clear();
        this.ignoredPairs.clear();

        this.allSelectedPositions.clear();
        this.closestSelectedPositions.clear();

        this.status = RunStatus.STOPPED;
    }

    protected void updateRequiredBoxes()
    {
        LayerRange range = DataManager.getRenderLayerRange();

        this.boxesInChunks.clear();

        for (SchematicPlacement placement : this.placements)
        {
            Collection<SelectionBox> boxes = placement.getSubRegionBoxes(EnabledCondition.ENABLED).values();

            if (this.verifierType == BlockInfoListType.RENDER_LAYERS)
            {
                PositionUtils.getLayerRangeClampedPerChunkBoxes(boxes, range, this.boxesInChunks::put);
            }
            else
            {
                PositionUtils.getPerChunkBoxes(boxes, this.boxesInChunks::put);
            }
        }

        SchematicVerifierManager.INSTANCE.updateTouchedChunks();
    }

    protected void updateSelectedPairsIfDirty()
    {
        if (this.selectedPairsDirty)
        {
            this.updateSelectedPairs();
        }
    }

    protected void updateSelectedPositionsIfDirty()
    {
        if (this.selectedPositionsDirty)
        {
            this.updateSelectedPositions();
        }
    }

    protected void updateCountsIfDirty()
    {
        if (this.countsDirty)
        {
            this.updateCounts();
        }
    }

    protected void updateCounts()
    {
        this.countsPerPair.clear();
        this.countsPerType.clear();

        for (Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> map : this.resultsPerChunk.values())
        {
            for (BlockStatePair pair : map.keySet())
            {
                int size = map.get(pair).size();
                this.countsPerPair.addTo(pair, size);
                this.countsPerType.addTo(pair.type, size);
            }
        }

        this.countsDirty = false;
    }

    protected void updateSelectedPairs()
    {
        this.updateCountsIfDirty();
        this.calculatedSelectedPairs.clear();

        for (BlockStatePair pair : this.countsPerPair.keySet())
        {
            if (this.selectedPairs.contains(pair) || this.selectedTypes.contains(pair.type))
            {
                this.calculatedSelectedPairs.add(pair);
            }
        }

        this.selectedPairsDirty = false;
        this.selectedPositionsDirty = true;
    }

    protected void updateSelectedPositions()
    {
        this.updateSelectedPairsIfDirty();
        this.allSelectedPositions.clear();

        for (long chunkPosLong : this.resultsPerChunk.keySet())
        {
            Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> results = this.resultsPerChunk.get(chunkPosLong);

            for (BlockStatePair pair : results.keySet())
            {
                if (this.calculatedSelectedPairs.contains(pair))
                {
                    for (int relPos : results.get(pair))
                    {
                        this.allSelectedPositions.add(BlockPairTypePosition.of(pair, chunkPosLong, relPos));
                    }
                }
            }
        }

        this.selectedPositionsDirty = false;
        this.selectedClosestPositionsDirty = true;
    }

    protected void updateClosestPositionsIfDirty(BlockPos referencePos)
    {
        this.updateSelectedPairsIfDirty();
        this.updateSelectedPositionsIfDirty();

        int hysteresis = 16;

        if (this.selectedClosestPositionsDirty || this.lastSortPosition == null ||
            Math.abs(this.lastSortPosition.getX() - referencePos.getX()) > hysteresis ||
            Math.abs(this.lastSortPosition.getY() - referencePos.getY()) > hysteresis ||
            Math.abs(this.lastSortPosition.getZ() - referencePos.getZ()) > hysteresis)
        {
            this.updateClosestPositions(referencePos);
            this.updateInfoHudLines();
        }
    }

    protected void updateClosestPositions(BlockPos referencePos)
    {
        this.closestSelectedPositions.clear();

        int max = Configs.InfoOverlays.VERIFIER_ERROR_HIGHLIGHT_MAX_POSITIONS.getIntegerValue();
        int endIndex = Math.min(max, this.allSelectedPositions.size());
        this.allSelectedPositions.sort(new BlockPairTypePositionComparator(referencePos, true));
        this.closestSelectedPositions.addAll(this.allSelectedPositions.subList(0, endIndex));
        this.selectedClosestPositionsDirty = false;
        this.lastSortPosition = referencePos;
    }

    protected void updateInfoHudLines()
    {
        int max = Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue();
        int endIndex = Math.min(max - 1, this.closestSelectedPositions.size());

        this.infoHudLines.clear();
        this.infoHudLines.add(StringUtils.translate("litematica.hud.task_name.schematic_verifier"));

        for (int i = 0; i < endIndex; ++i)
        {
            BlockPairTypePosition pair = this.closestSelectedPositions.get(i);
            int x = malilib.util.position.PositionUtils.unpackX(pair.posLong);
            int y = malilib.util.position.PositionUtils.unpackY(pair.posLong);
            int z = malilib.util.position.PositionUtils.unpackZ(pair.posLong);
            String name;

            if (pair.type == VerifierResultType.EXTRA)
            {
                name = RegistryUtils.getBlockId(pair.pair.foundState).getPath();
            }
            else
            {
                name = RegistryUtils.getBlockId(pair.pair.expectedState).getPath();
            }

            // TODO switch to StyledTextLine list with a starting style
            String colorTag = "<c=" + String.format("%08X", pair.type.getTextColor()) + ">";
            this.infoHudLines.add(StringUtils.translate("litematica.hud.schematic_verifier.position",
                                                        colorTag, name, x, y, z));
        }
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase)
    {
        return this.infoHudEnabled && phase == RenderPhase.POST &&
               this.closestSelectedPositions.isEmpty() == false;
    }

    @Override
    public List<String> getText(RenderPhase phase)
    {
        return phase == RenderPhase.POST ? this.infoHudLines : Collections.emptyList();
    }
}
