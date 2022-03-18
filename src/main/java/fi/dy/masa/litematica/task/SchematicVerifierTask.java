package fi.dy.masa.litematica.task;

import com.google.common.collect.ArrayListMultimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.litematica.scheduler.tasks.TaskProcessChunkBase;
import fi.dy.masa.litematica.schematic.verifier.BlockStatePair;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.VerifierResultType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.position.IntBoundingBox;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class SchematicVerifierTask extends TaskProcessChunkBase
{
    protected static final IBlockState AIR = Blocks.AIR.getDefaultState();

    protected final SchematicVerifier verifier;
    protected final WorldSchematic schematicWorld;

    public SchematicVerifierTask(SchematicVerifier verifier)
    {
        super("litematica.title.hud.schematic_verifier");

        this.verifier = verifier;
        this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
    }

    public void setBoxes(ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks)
    {
        this.requiredChunks.clear();
        this.boxesInChunks.clear();
        this.boxesInChunks.putAll(boxesInChunks);
        this.requiredChunks.addAll(this.boxesInChunks.keySet());
    }

    @Override
    public boolean canExecute()
    {
        return this.worldClient != null && this.schematicWorld != null;
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.worldClient, 1) &&
               this.schematicWorld.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        this.checkBlocksInChunk(pos);
        return true;
    }

    protected void checkBlocksInChunk(ChunkPos pos)
    {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Chunk schematicChunk = this.schematicWorld.getChunk(pos.x, pos.z);
        Chunk clientChunk = this.worldClient.getChunk(pos.x, pos.z);
        Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> results = new Object2ObjectOpenHashMap<>();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = bb.minX;
            final int startY = bb.minY;
            final int startZ = bb.minZ;
            final int endX = bb.maxX;
            final int endY = bb.maxY;
            final int endZ = bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        mutablePos.setPos(x, y, z);
                        this.checkBlock(mutablePos, schematicChunk, clientChunk, results);
                    }
                }
            }
        }

        this.verifier.addBlockResultsFromWorld(pos, results);
    }

    protected void checkBlock(BlockPos.MutableBlockPos pos,
                              Chunk schematicChunk,
                              Chunk clientChunk,
                              Object2ObjectOpenHashMap<BlockStatePair, IntArrayList> results)
    {
        IBlockState clientBlock = clientChunk.getBlockState(pos).getActualState(clientChunk.getWorld(), pos);
        IBlockState schematicBlock = schematicChunk.getBlockState(pos);
        VerifierResultType type = VerifierResultType.from(schematicBlock, clientBlock);
        BlockStatePair pair = new BlockStatePair(type, schematicBlock, clientBlock);
        int posInt = PositionUtils.getPackedChunkRelativePosition(pos);

        results.computeIfAbsent(pair, (p) -> new IntArrayList()).add(posInt);
    }
}
