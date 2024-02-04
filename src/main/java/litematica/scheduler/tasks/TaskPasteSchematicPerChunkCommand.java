package litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.chunk.Chunk;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.game.wrap.EntityWrap;
import malilib.util.game.wrap.GameWrap;
import malilib.util.game.wrap.RegistryUtils;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.IntBoundingBox;
import malilib.util.position.LayerRange;
import litematica.config.Configs;
import litematica.schematic.placement.SchematicPlacement;
import litematica.util.value.ReplaceBehavior;
import litematica.world.SchematicWorldHandler;
import litematica.world.WorldSchematic;

public class TaskPasteSchematicPerChunkCommand extends TaskPasteSchematicPerChunkBase
{
    protected final List<IntBoundingBox> boxesInCurrentChunk = new ArrayList<>();
    private final int maxCommandsPerTick;
    private int sentCommandsThisTick;
    private int sentCommandsTotal;
    private int currentX;
    private int currentY;
    private int currentZ;
    private int currentIndex;
    private int boxVolume;
    private boolean boxInProgress;

    public TaskPasteSchematicPerChunkCommand(Collection<SchematicPlacement> placements, LayerRange range, boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        this.maxCommandsPerTick = Configs.Generic.PASTE_COMMAND_LIMIT.getIntegerValue();
    }

    @Override
    public boolean execute()
    {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        WorldClient worldClient = this.mc.world;
        this.sentCommandsThisTick = 0;
        int processed = 0;
        int chunkAttempts = 0;

        if (this.sentCommandsTotal == 0)
        {
            GameWrap.sendChatMessage("/gamerule sendCommandFeedback false");
        }

        while (this.chunks.isEmpty() == false)
        {
            ChunkPos pos = this.chunks.get(0);

            if (this.canProcessChunk(pos, worldSchematic, worldClient) == false)
            {
                // There is already a box in progress, we must finish that before
                // moving to the next box
                if (this.boxInProgress || chunkAttempts > 0)
                {
                    return false;
                }
                else
                {
                    this.sortChunkList();
                    ++chunkAttempts;
                    continue;
                }
            }

            while (this.boxesInCurrentChunk.isEmpty() == false)
            {
                IntBoundingBox box = this.boxesInCurrentChunk.get(0);

                if (this.processBox(pos, box, worldSchematic, worldClient, this.mc.player))
                {
                    this.boxesInCurrentChunk.remove(0);

                    if (this.boxesInCurrentChunk.isEmpty())
                    {
                        this.boxesInChunks.removeAll(pos);
                        this.chunks.remove(0);
                        ++processed;

                        if (this.chunks.isEmpty())
                        {
                            this.finished = true;
                            return true;
                        }

                        this.sortChunkList();

                        // break to fetch the next chunk
                        break;
                    }
                }
                // Don't allow processing other boxes when the first one is still incomplete
                else
                {
                    if (processed > 0)
                    {
                        this.updateInfoHudLines();
                    }

                    return false;
                }
            }
        }

        if (processed > 0)
        {
            this.updateInfoHudLines();
        }

        return false;
    }

    @Override
    protected void onChunkListSorted()
    {
        super.onChunkListSorted();

        this.boxesInCurrentChunk.clear();
        this.boxesInCurrentChunk.addAll(this.boxesInChunks.get(this.chunks.get(0)));
    }

    protected boolean processBox(ChunkPos pos, IntBoundingBox box,
            WorldSchematic worldSchematic, WorldClient worldClient, EntityPlayerSP player)
    {
        BlockPos.MutBlockPos posMutable = new BlockPos.MutBlockPos();
        Chunk chunkSchematic = worldSchematic.getChunkProvider().getLoadedChunk(pos.x, pos.z);
        Chunk chunkClient = worldClient.getChunkProvider().getLoadedChunk(pos.x, pos.z);

        if (this.boxInProgress == false)
        {
            this.currentX = box.minX;
            this.currentY = box.minY;
            this.currentZ = box.minZ;
            this.boxVolume = (box.maxX - box.minX + 1) * (box.maxY - box.minY + 1) * (box.maxZ - box.minZ + 1);
            this.currentIndex = 0;
            this.boxInProgress = true;
        }

        while (this.currentIndex < this.boxVolume)
        {
            posMutable.set(this.currentX, this.currentY, this.currentZ);

            if (++this.currentY > box.maxY)
            {
                this.currentY = box.minY;

                if (++this.currentX > box.maxX)
                {
                    this.currentX = box.minX;
                    ++this.currentZ;
                }
            }

            ++this.currentIndex;

            IBlockState stateSchematicOrig = chunkSchematic.getBlockState(posMutable);
            IBlockState stateClient = chunkClient.getBlockState(posMutable);

            if (stateSchematicOrig.getBlock() != Blocks.AIR || stateClient.getBlock() != Blocks.AIR)
            {
                // Discard the non-meta state info, as it depends on neighbor blocks which will
                // be synced with some delay from the server. TODO 1.13 remove this
                @SuppressWarnings("deprecation")
                IBlockState stateSchematic = stateSchematicOrig.getBlock().getStateFromMeta(stateSchematicOrig.getBlock().getMetaFromState(stateSchematicOrig));

                if (this.changedBlockOnly == false || stateClient != stateSchematic)
                {
                    if ((this.replace == ReplaceBehavior.NONE && stateClient.getMaterial() != Material.AIR) ||
                        (this.replace == ReplaceBehavior.WITH_NON_AIR && stateSchematicOrig.getMaterial() == Material.AIR))
                    {
                        continue;
                    }

                    this.sendSetBlockCommand(posMutable.getX(), posMutable.getY(), posMutable.getZ(), stateSchematicOrig);

                    if (++this.sentCommandsThisTick >= this.maxCommandsPerTick)
                    {
                        break;
                    }
                }
            }
        }

        if (this.currentIndex >= this.boxVolume)
        {
            this.summonEntities(box, worldSchematic);
            this.boxInProgress = false;

            return true;
        }

        return false;
    }

    private void summonEntities(IntBoundingBox box, WorldSchematic worldSchematic)
    {
        AxisAlignedBB bb = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        List<Entity> entities = worldSchematic.getEntitiesWithinAABBExcludingEntity(null, bb);

        for (Entity entity : entities)
        {
            ResourceLocation rl = EntityList.getKey(entity);

            if (rl != null)
            {
                String entityName = rl.toString();
                /*
                NBTTagCompound nbt = new NBTTagCompound();
                entity.writeToNBTOptional(nbt);
                String nbtString = nbt.toString();
                */

                String strCommand = String.format(Locale.ROOT, "/summon %s %f %f %f", entityName,
                                                  EntityWrap.getX(entity), EntityWrap.getY(entity), EntityWrap.getZ(entity));
                /*
                String strCommand = String.format("/summon %s %f %f %f %s", entityName, entity.posX, entity.posY, entity.posZ, nbtString);
                System.out.printf("entity: %s\n", entity);
                System.out.printf("%s\n", strCommand);
                System.out.printf("nbt: %s\n", nbtString);
                */

                GameWrap.sendChatMessage(strCommand);
            }
        }
    }

    private void sendSetBlockCommand(int x, int y, int z, IBlockState state)
    {
        Block block = state.getBlock();
        String blockName = RegistryUtils.getBlockIdStr(block);

        if (blockName == null)
        {
            return;
        }

        String cmdName = Configs.Generic.COMMAND_NAME_SETBLOCK.getValue();
        String strCommand = String.format("/%s %d %d %d %s %d", cmdName, x, y, z, blockName, block.getMetaFromState(state));

        GameWrap.sendChatMessage(strCommand);
        ++this.sentCommandsTotal;
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                MessageDispatcher.success().screenOrActionbar().translate("litematica.message.schematic_pasted_using_setblock", this.sentCommandsTotal);
            }
        }
        else
        {
            MessageDispatcher.error().screenOrActionbar().translate("litematica.message.error.schematic_paste_failed");
        }

        GameWrap.sendCommand("/gamerule sendCommandFeedback true");

        super.stop();
    }
}
