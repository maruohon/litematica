package fi.dy.masa.litematica.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase;
import fi.dy.masa.litematica.gui.base.GuiLitematicaBase.InfoType;
import fi.dy.masa.litematica.render.IStringListProvider;
import fi.dy.masa.litematica.render.InfoHud;
import fi.dy.masa.litematica.util.ItemUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.Vec4f;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public class SchematicVerifier implements IStringListProvider
{
    private static final MutablePair<IBlockState, IBlockState> MUTABLE_PAIR = new MutablePair<>();
    private static final BlockPos.MutableBlockPos MUTABLE_POS = new BlockPos.MutableBlockPos();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static long clientTickStart;
    private static boolean verifierActive;

    private final ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> missingBlocksPositions = ArrayListMultimap.create();
    private final ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> extraBlocksPositions = ArrayListMultimap.create();
    private final ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> wrongBlocksPositions = ArrayListMultimap.create();
    private final ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> wrongStatesPositions = ArrayListMultimap.create();
    private final Object2ObjectOpenHashMap<BlockPos, BlockMismatch> blockMismatches = new Object2ObjectOpenHashMap<>();
    private final HashSet<Pair<IBlockState, IBlockState>> ignoredMismatches = new HashSet<>();
    private final List<BlockPos> missingBlocksPositionsClosest = new ArrayList<>();
    private final List<BlockPos> extraBlocksPositionsClosest = new ArrayList<>();
    private final List<BlockPos> mismatchedBlocksPositionsClosest = new ArrayList<>();
    private final List<BlockPos> mismatchedStatesPositionsClosest = new ArrayList<>();
    private final List<String> infoLines = new ArrayList<>();
    private final Set<ChunkPos> requiredChunks = new HashSet<>();
    private final Set<BlockPos> recheckQueue = new HashSet<>();
    private WorldClient worldClient;
    private WorldSchematic worldSchematic;
    private SchematicPlacement schematicPlacement;
    @Nullable
    private ICompletionListener completionListener;
    private boolean verificationActive;
    private boolean finished;
    private int totalRequiredChunks;
    private long schematicBlocks;
    private long clientBlocks;
    private long matchingBlocks;

    public static boolean isVerifierActive()
    {
        return verifierActive;
    }

    @Override
    public List<String> getLines()
    {
        return this.infoLines;
    }

    public static void onClientTickStart()
    {
        clientTickStart = System.nanoTime();
    }

    public boolean isActive()
    {
        return this.verificationActive;
    }

    public int getTotalChunks()
    {
        return this.totalRequiredChunks;
    }

    public int getUnseenChunks()
    {
        return this.requiredChunks.size();
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public long getSchematicTotalBlocks()
    {
        return this.schematicBlocks;
    }

    public long getRealWorldTotalBlocks()
    {
        return this.clientBlocks;
    }

    public long getMissingBlocks()
    {
        return this.missingBlocksPositions.size();
    }

    public long getExtraBlocks()
    {
        return this.extraBlocksPositions.size();
    }

    public long getMatchingBlocks()
    {
        return this.matchingBlocks;
    }

    public long getMismatchedBlocks()
    {
        return this.wrongBlocksPositions.size();
    }

    public long getMismatchedStates()
    {
        return this.wrongStatesPositions.size();
    }

    public void reset()
    {
        this.stopVerification();
    }

    public void startVerification(WorldClient worldClient, WorldSchematic worldSchematic,
            SchematicPlacement schematicPlacement, ICompletionListener completionListener)
    {
        this.stopVerification();

        this.worldClient = worldClient;
        this.worldSchematic = worldSchematic;
        this.schematicPlacement = schematicPlacement;

        this.requiredChunks.addAll(schematicPlacement.getTouchedChunks());
        this.totalRequiredChunks = this.requiredChunks.size();
        this.completionListener = completionListener;
        this.verificationActive = true;
    }

    public void stopVerification()
    {
        this.worldClient = null;
        this.worldSchematic = null;
        this.schematicPlacement = null;
        this.totalRequiredChunks = 0;
        this.requiredChunks.clear();

        this.missingBlocksPositions.clear();
        this.extraBlocksPositions.clear();
        this.wrongBlocksPositions.clear();
        this.wrongStatesPositions.clear();
        this.blockMismatches.clear();

        this.verificationActive = false;
        this.finished = false;
        verifierActive = false;

        InfoHud.getInstance().removeLineProvider(this);
        DataManager.clearActiveMismatchPositions();
    }

    public void markBlockChanged(BlockPos pos)
    {
        if (this.finished)
        {
            BlockMismatch mismatch = this.blockMismatches.get(pos);

            if (mismatch != null)
            {
                this.recheckQueue.add(pos);
            }
        }
    }

    public void checkChangedPositions()
    {
        if (this.finished && this.recheckQueue.isEmpty() == false)
        {
            for (BlockPos pos : this.recheckQueue)
            {
                BlockMismatch mismatch = this.blockMismatches.get(pos);

                if (mismatch != null)
                {
                    this.blockMismatches.remove(pos);

                    IBlockState stateFound = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);
                    MUTABLE_PAIR.setLeft(mismatch.stateExpected);
                    MUTABLE_PAIR.setRight(mismatch.stateFound);

                    this.getMapForMismatchType(mismatch.mismatchType).remove(MUTABLE_PAIR, pos);
                    this.checkBlockStates(pos.getX(), pos.getY(), pos.getZ(), mismatch.stateExpected, stateFound);

                    if (stateFound != AIR && mismatch.stateFound == AIR)
                    {
                        this.clientBlocks++;
                    }
                }
                else
                {
                    IBlockState stateExpected = this.worldSchematic.getBlockState(pos);
                    IBlockState stateFound = this.worldClient.getBlockState(pos).getActualState(this.worldClient, pos);
                    this.checkBlockStates(pos.getX(), pos.getY(), pos.getZ(), stateExpected, stateFound);
                }
            }

            this.updateClosestPositions(10);

            MismatchType mismatchType = DataManager.getSelectedMismatchType();

            if (mismatchType != null)
            {
                List<BlockPos> list = this.getClosestMismatchedPositionsFor(mismatchType);
                DataManager.setActiveMismatchPositions(mismatchType, list);
                this.getClosestMismatchedPositionListProviderFor(mismatchType);
            }

            this.recheckQueue.clear();
        }
    }

    private ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> getMapForMismatchType(MismatchType mismatchType)
    {
        switch (mismatchType)
        {
            case MISSING:
                return this.missingBlocksPositions;
            case EXTRA:
                return this.extraBlocksPositions;
            case WRONG_BLOCK:
                return this.wrongBlocksPositions;
            case WRONG_STATE:
                return this.wrongStatesPositions;
            default:
                return null;
        }
    }

    public boolean verifyChunks()
    {
        if (this.verificationActive)
        {
            Iterator<ChunkPos> iter = this.requiredChunks.iterator();

            while (iter.hasNext())
            {
                if ((System.nanoTime() - clientTickStart) >= 50000000L)
                {
                    break;
                }

                ChunkPos pos = iter.next();

                if (this.worldClient.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z) &&
                    this.worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z))
                {
                    Chunk chunkClient = this.worldClient.getChunkFromChunkCoords(pos.x, pos.z);
                    Chunk chunkSchematic = this.worldSchematic.getChunkFromChunkCoords(pos.x, pos.z);
                    Map<String, StructureBoundingBox> boxes = this.schematicPlacement.getBoxesWithinChunk(pos.x, pos.z);

                    for (StructureBoundingBox box : boxes.values())
                    {
                        this.verifyChunk(chunkClient, chunkSchematic, box);
                    }

                    iter.remove();
                }
            }

            if (this.requiredChunks.isEmpty())
            {
                this.verificationActive = false;
                this.finished = true;
                verifierActive = true;

                if (this.completionListener != null)
                {
                    this.completionListener.onTaskCompleted();
                }
            }
        }

        return this.verificationActive == false; // finished or stopped
    }

    public void ignoreStateMismatch(BlockMismatch mismatch)
    {
        Pair<IBlockState, IBlockState> ignore = Pair.of(mismatch.stateExpected, mismatch.stateFound);

        this.ignoredMismatches.add(ignore);
        this.getMapForMismatchType(mismatch.mismatchType).removeAll(ignore);

        Iterator<Map.Entry<BlockPos, BlockMismatch>> iter = this.blockMismatches.entrySet().iterator();

        while (iter.hasNext())
        {
            Map.Entry<BlockPos, BlockMismatch> entry = iter.next();

            if (entry.getValue().equals(mismatch))
            {
                iter.remove();
            }
        }
    }

    public void addIgnoredStateMismatches(Collection<BlockMismatch> ignore)
    {
        for (BlockMismatch mismatch : ignore)
        {
            this.ignoreStateMismatch(mismatch);
        }
    }

    public void setIgnoredStateMismatches(Collection<BlockMismatch> ignore)
    {
        this.ignoredMismatches.clear();
        this.addIgnoredStateMismatches(ignore);
    }

    @Nullable
    public BlockMismatch getMismatchForPosition(BlockPos pos)
    {
        return this.blockMismatches.get(pos);
    }

    public List<BlockMismatch> getMismatchOverviewCombined()
    {
        List<BlockMismatch> list = new ArrayList<>();

        this.addCountFor(MismatchType.MISSING, this.missingBlocksPositions, list);
        this.addCountFor(MismatchType.EXTRA, this.extraBlocksPositions, list);
        this.addCountFor(MismatchType.WRONG_BLOCK, this.wrongBlocksPositions, list);
        this.addCountFor(MismatchType.WRONG_STATE, this.wrongStatesPositions, list);

        Collections.sort(list);

        return list;
    }

    public List<BlockMismatch> getMismatchOverviewFor(MismatchType type)
    {
        List<BlockMismatch> list = new ArrayList<>();

        if (type == MismatchType.ALL)
        {
            return this.getMismatchOverviewCombined();
        }
        else
        {
            this.addCountFor(type, this.getMapForMismatchType(type), list);
        }

        return list;
    }

    private void addCountFor(MismatchType mismatchType, ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> map, List<BlockMismatch> list)
    {
        for (Pair<IBlockState, IBlockState> pair : map.keySet())
        {
            list.add(new BlockMismatch(mismatchType, pair.getLeft(), pair.getRight(), map.get(pair).size()));
        }
    }

    public List<BlockPos> getClosestMismatchedPositionsFor(MismatchType type)
    {
        switch (type)
        {
            //case ALL:
            //    return Collections.emptyList();
            case MISSING:
                return this.missingBlocksPositionsClosest;
            case EXTRA:
                return this.extraBlocksPositionsClosest;
            case WRONG_BLOCK:
                return this.mismatchedBlocksPositionsClosest;
            case WRONG_STATE:
                return this.mismatchedStatesPositionsClosest;
            default:
                return Collections.emptyList();
        }
    }

    /**
     * Prepares/caches the strings, and returns a provider for the data.<br>
     * <b>NOTE:</b> This is actually the instance of this class, there are no separate providers for different data types atm!
     * @param type
     * @return
     */
    public IStringListProvider getClosestMismatchedPositionListProviderFor(MismatchType type)
    {
        this.infoLines.clear();
        List<BlockPos> list = this.getClosestMismatchedPositionsFor(type);

        if (list.isEmpty() == false)
        {
            this.infoLines.add(String.format("%s%s%s", type.getFormattingCode(), type.getDisplayname(), TextFormatting.RESET.toString()));

            for (BlockPos pos : list)
            {
                this.infoLines.add(String.format("x: %6d, y: %3d, z: %6d", pos.getX(), pos.getY(), pos.getZ()));
            }
        }

        return this;
    }

    public List<Pair<IBlockState, IBlockState>> getIgnoredStateMismatchPairs(GuiLitematicaBase gui)
    {
        List<Pair<IBlockState, IBlockState>> list = Lists.newArrayList(this.ignoredMismatches);

        try
        {
            Collections.sort(list, new Comparator<Pair<IBlockState, IBlockState>>() {
                @Override
                public int compare(Pair<IBlockState, IBlockState> o1, Pair<IBlockState, IBlockState> o2)
                {
                    String name1 = Block.REGISTRY.getNameForObject(o1.getLeft().getBlock()).toString();
                    String name2 = Block.REGISTRY.getNameForObject(o2.getLeft().getBlock()).toString();

                    int val = name1.compareTo(name2);

                    if (val < 0)
                    {
                        return -1;
                    }
                    else if (val > 0)
                    {
                        return 1;
                    }
                    else
                    {
                        name1 = Block.REGISTRY.getNameForObject(o1.getRight().getBlock()).toString();
                        name2 = Block.REGISTRY.getNameForObject(o2.getRight().getBlock()).toString();

                        return name1.compareTo(name2);
                    }
                }
            });
        }
        catch (Exception e)
        {
            gui.addMessage(InfoType.ERROR, "litematica.error.generic.failed_to_sort_list_of_ignored_states");
        }

        return list;
    }

    private void verifyChunk(Chunk chunkClient, Chunk chunkSchematic, StructureBoundingBox box)
    {
        for (int y = box.minY; y <= box.maxY; ++y)
        {
            for (int z = box.minZ; z <= box.maxZ; ++z)
            {
                for (int x = box.minX; x <= box.maxX; ++x)
                {
                    MUTABLE_POS.setPos(x, y, z);
                    IBlockState stateClient = chunkClient.getBlockState(x, y, z).getActualState(chunkClient.getWorld(), MUTABLE_POS);
                    IBlockState stateSchematic = chunkSchematic.getBlockState(x, y, z);

                    this.checkBlockStates(x, y, z, stateSchematic, stateClient);

                    if (stateSchematic != AIR)
                    {
                        this.schematicBlocks++;
                    }

                    if (stateClient != AIR)
                    {
                        this.clientBlocks++;
                    }
                }
            }
        }
    }

    private void checkBlockStates(int x, int y, int z, IBlockState stateSchematic, IBlockState stateClient)
    {
        if (stateClient != stateSchematic)
        {
            MUTABLE_PAIR.setLeft(stateSchematic);
            MUTABLE_PAIR.setRight(stateClient);

            if (this.ignoredMismatches.contains(MUTABLE_PAIR) == false)
            {
                BlockPos pos = new BlockPos(x, y, z);
                BlockMismatch mismatch;

                if (stateSchematic != AIR)
                {
                    if (stateClient == AIR)
                    {
                        mismatch = new BlockMismatch(MismatchType.MISSING, stateSchematic, stateClient, 1);
                        this.missingBlocksPositions.put(Pair.of(stateSchematic, stateClient), pos);
                    }
                    else
                    {
                        if (stateSchematic.getBlock() != stateClient.getBlock())
                        {
                            mismatch = new BlockMismatch(MismatchType.WRONG_BLOCK, stateSchematic, stateClient, 1);
                            this.wrongBlocksPositions.put(Pair.of(stateSchematic, stateClient), pos);
                        }
                        else
                        {
                            mismatch = new BlockMismatch(MismatchType.WRONG_STATE, stateSchematic, stateClient, 1);
                            this.wrongStatesPositions.put(Pair.of(stateSchematic, stateClient), pos);
                        }
                    }
                }
                else
                {
                    mismatch = new BlockMismatch(MismatchType.EXTRA, stateSchematic, stateClient, 1);
                    this.extraBlocksPositions.put(Pair.of(stateSchematic, stateClient), pos);
                }

                this.blockMismatches.put(pos, mismatch);

                ItemUtils.setItemForBlock(this.worldClient, pos, stateClient);
                ItemUtils.setItemForBlock(this.worldSchematic, pos, stateSchematic);
            }
        }
        else
        {
            this.matchingBlocks++;
        }
    }

    public void updateClosestPositions(int maxEntries)
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.player != null)
        {
            BlockPos posPlayer = new BlockPos(mc.player.getPositionVector());
            PositionUtils.BLOCK_POS_COMPARATOR.setReferencePosition(posPlayer);
            PositionUtils.BLOCK_POS_COMPARATOR.setClosestFirst(true);

            this.addAndSortPositions(this.wrongBlocksPositions, this.mismatchedBlocksPositionsClosest, maxEntries);
            this.addAndSortPositions(this.wrongStatesPositions, this.mismatchedStatesPositionsClosest, maxEntries);
            this.addAndSortPositions(this.extraBlocksPositions, this.extraBlocksPositionsClosest, maxEntries);
            this.addAndSortPositions(this.missingBlocksPositions, this.missingBlocksPositionsClosest, maxEntries);
        }
    }

    private void addAndSortPositions(ArrayListMultimap<Pair<IBlockState, IBlockState>, BlockPos> sourceMap, List<BlockPos> listOut, int maxEntries)
    {
        listOut.clear();

        List<BlockPos> tempList = new ArrayList<>();
        tempList.addAll(sourceMap.values());
        Collections.sort(tempList, PositionUtils.BLOCK_POS_COMPARATOR);

        final int max = Math.min(maxEntries, tempList.size());

        for (int i = 0; i < max; ++i)
        {
            listOut.add(tempList.get(i));
        }
    }

    public static class BlockMismatch implements Comparable<BlockMismatch>
    {
        public final MismatchType mismatchType;
        public final IBlockState stateExpected;
        public final IBlockState stateFound;
        public final int count;

        public BlockMismatch(MismatchType mismatchType, IBlockState stateExpected, IBlockState stateFound, int count)
        {
            this.mismatchType = mismatchType;
            this.stateExpected = stateExpected;
            this.stateFound = stateFound;
            this.count = count;
        }

        @Override
        public int compareTo(BlockMismatch other)
        {
            return this.count > other.count ? -1 : (this.count < other.count ? 1 : 0);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((mismatchType == null) ? 0 : mismatchType.hashCode());
            result = prime * result + ((stateExpected == null) ? 0 : stateExpected.hashCode());
            result = prime * result + ((stateFound == null) ? 0 : stateFound.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BlockMismatch other = (BlockMismatch) obj;
            if (mismatchType != other.mismatchType)
                return false;
            if (stateExpected == null)
            {
                if (other.stateExpected != null)
                    return false;
            }
            else if (stateExpected != other.stateExpected)
                return false;
            if (stateFound == null)
            {
                if (other.stateFound != null)
                    return false;
            }
            else if (stateFound != other.stateFound)
                return false;
            return true;
        }
    }

    public enum MismatchType
    {
        ALL         (0xFF0000, "litematica.gui.label.schematic_verifier_display_type.all", TextFormatting.WHITE.toString() + TextFormatting.BOLD),
        MISSING     (0x00FFFF, "litematica.gui.label.schematic_verifier_display_type.missing", TextFormatting.AQUA.toString() + TextFormatting.BOLD),
        EXTRA       (0xFF00CF, "litematica.gui.label.schematic_verifier_display_type.extra", TextFormatting.LIGHT_PURPLE.toString() + TextFormatting.BOLD),
        WRONG_BLOCK (0xFF0000, "litematica.gui.label.schematic_verifier_display_type.wrong_blocks", TextFormatting.RED.toString() + TextFormatting.BOLD),
        WRONG_STATE (0xFFAF00, "litematica.gui.label.schematic_verifier_display_type.wrong_state", TextFormatting.GOLD.toString() + TextFormatting.BOLD);

        private final String unlocName;
        private final String formattingCode;
        private final Vec4f color;

        private MismatchType(int color, String unlocName, String formattingCode)
        {
            this.color = Vec4f.fromColor(color, 1f);
            this.unlocName = unlocName;
            this.formattingCode = formattingCode;
        }

        public Vec4f getColor()
        {
            return this.color;
        }

        public String getDisplayname()
        {
            return I18n.format(this.unlocName);
        }

        public String getFormattingCode()
        {
            return this.formattingCode;
        }
    }
}
