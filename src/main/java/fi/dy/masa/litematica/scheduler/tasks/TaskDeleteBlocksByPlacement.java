package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.PlacementDeletionMode;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;

public class TaskDeleteBlocksByPlacement extends TaskProcessChunkMultiPhase
{
    protected static final BlockState AIR = Blocks.AIR.getDefaultState();

    protected final ImmutableList<SchematicPlacement> placements;
    protected final LayerRange layerRange;
    protected final PlacementDeletionMode mode;
    protected final String setBlockCommand;
    protected final String blockString;
    protected long blockCount;

    public TaskDeleteBlocksByPlacement(Collection<SchematicPlacement> placements,
                                       PlacementDeletionMode mode,
                                       LayerRange layerRange)
    {
        super("Delete Blocks");

        this.placements = ImmutableList.copyOf(placements);
        this.mode = mode;
        this.layerRange = layerRange;
        this.setBlockCommand = Configs.Generic.COMMAND_NAME_SETBLOCK.getStringValue();
        this.blockString = BlockArgumentParser.stringifyBlockState(Blocks.AIR.getDefaultState());
        this.processBoxBlocksTask = this::sendQueuedCommands;
    }

    @Override
    public boolean canExecute()
    {
        return super.canExecute() && this.schematicWorld != null;
    }

    protected void onChunkAddedForHandling(ChunkPos pos, SchematicPlacement placement)
    {
        //this.placementsPerChunk.put(pos, placement);
    }

    protected void addPlacement(SchematicPlacement placement, LayerRange range)
    {
        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();

        for (ChunkPos pos : touchedChunks)
        {
            int count = 0;

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
            {
                box = PositionUtils.getClampedBox(box, range);

                if (box != null)
                {
                    // Clamp the box to the world bounds.
                    box = PositionUtils.clampBoxToWorldHeightRange(box, this.clientWorld);

                    if (box != null)
                    {
                        this.boxesInChunks.put(pos, box);
                        ++count;
                    }
                }
            }

            if (count > 0)
            {
                this.onChunkAddedForHandling(pos, placement);
            }
        }
    }

    @Override
    public void init()
    {
        if (this.useWorldEdit && this.isInWorld())
        {
            this.sendCommand("/perf neighbors off");
        }

        for (SchematicPlacement placement : this.placements)
        {
            this.addPlacement(placement, this.layerRange);
        }

        this.pendingChunks.clear();
        this.pendingChunks.addAll(this.boxesInChunks.keySet());
        this.sortChunkList();
    }

    @Override
    public boolean execute()
    {
        return this.executeMultiPhase();
    }

    @Override
    protected void onNextChunkFetched(ChunkPos pos)
    {
        if (this.isClientWorld)
        {
            this.queueCommandsForBoxesInChunk(pos);
        }
        else
        {
            this.directRemoveBoxesInChunk(pos);
        }
    }

    protected void queueCommandsForBoxesInChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            this.removeEntitiesByCommand(box);
            this.removeBlocksInBox(box, this.mode, this::removeBlockByCommand);
        }

        // Use this phase to send the queued commands
        this.phase = TaskPhase.PROCESS_BOX_BLOCKS;
    }

    protected void directRemoveBoxesInChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            TaskFillArea.directRemoveEntities(box, this.world);
            this.removeBlocksInBox(box, this.mode, this::removeBlockDirect);
        }

        this.finishProcessingChunk(pos);
    }

    protected void removeBlocksInBox(IntBoundingBox box, PlacementDeletionMode mode, Consumer<BlockPos> removeFunc)
    {
        BlockCheck check = this.getCheckFor(mode);
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        for (int y = box.maxY; y >= box.minY; --y)
        {
            for (int x = box.minX; x <= box.maxX; ++x)
            {
                for (int z = box.minZ; z <= box.maxZ; ++z)
                {
                    posMutable.set(x, y, z);

                    if (this.world.getBlockState(posMutable) != AIR &&
                        check.shouldDelete(posMutable, this.schematicWorld, this.world))
                    {
                        removeFunc.accept(posMutable);
                        this.removeBlockDirect(posMutable);
                        ++this.blockCount;
                    }
                }
            }
        }
    }

    protected void removeBlockDirect(BlockPos pos)
    {
        BlockEntity te = this.world.getBlockEntity(pos);

        if (te instanceof Inventory)
        {
            ((Inventory) te).clear();
            this.world.setBlockState(pos, Blocks.BARRIER.getDefaultState(), 0x32);
        }

        this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 0x32);
    }

    protected void removeEntitiesByCommand(IntBoundingBox box)
    {
        String killCmd = String.format("kill @e[type=!player,x=%d,y=%d,z=%d,dx=%d,dy=%d,dz=%d]",
                                       box.minX               , box.minY               , box.minZ,
                                       box.maxX - box.minX + 1, box.maxY - box.minY + 1, box.maxZ - box.minZ + 1);

        this.queuedCommands.offer(killCmd);
    }

    protected void removeBlockByCommand(BlockPos pos)
    {
        if (this.useWorldEdit)
        {
            this.queuedCommands.offer(String.format("/pos1 %d,%d,%d", pos.getX(), pos.getY(), pos.getZ()));
            this.queuedCommands.offer(String.format("/pos2 %d,%d,%d", pos.getX(), pos.getY(), pos.getZ()));
            this.queuedCommands.offer("/set " + this.blockString);
        }
        else
        {
            final String cmdName = this.setBlockCommand;
            String fillCommand = String.format("%s %d %d %d %s",
                                               cmdName, pos.getX(), pos.getY(), pos.getZ(), this.blockString);

            this.queuedCommands.offer(fillCommand);
        }
    }

    protected BlockCheck getCheckFor(PlacementDeletionMode mode)
    {
        switch (mode)
        {
            case MATCHING_BLOCK:
                return (pos, sw, w) -> {
                    BlockState stateSchematic = sw.getBlockState(pos);
                    return stateSchematic != AIR && stateSchematic == w.getBlockState(pos);
                };
            case NON_MATCHING_BLOCK:
                return (pos, sw, w) -> {
                    BlockState stateSchematic = sw.getBlockState(pos);
                    return stateSchematic != AIR && stateSchematic != w.getBlockState(pos);
                };
            case ANY_SCHEMATIC_BLOCK:
                return (pos, sw, w) -> sw.getBlockState(pos) != Blocks.AIR.getDefaultState();
            case NO_SCHEMATIC_BLOCK:
                return (pos, sw, w) -> sw.getBlockState(pos) == Blocks.AIR.getDefaultState();
            default:
        }

        return (pos, sw, w) -> true;
    }

    protected interface BlockCheck
    {
        boolean shouldDelete(BlockPos pos, World schematicWorld, World world);
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        if (this.schematicWorld.getChunkProvider().isChunkLoaded(pos.x, pos.z) == false)
        {
            return false;
        }

        // Chunk exists in the schematic world, and all the surrounding chunks are loaded in the client world, good to go
        return this.areSurroundingChunksLoaded(pos, this.clientWorld, 1);
    }

    @Override
    protected void onStop()
    {
        if (this.finished)
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, String.format("Deleted %d blocks", this.blockCount));
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "Deletion task failed");
        }

        this.sendTaskEndCommands();

        DataManager.removeChatListener(this.gameRuleListener);
        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }
}
