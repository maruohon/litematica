package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
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
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;

public class TaskPasteSchematicSetblock extends TaskBase implements IInfoHudRenderer
{
    private final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    private final List<IntBoundingBox> boxesInCurrentChunk = new ArrayList<>();
    private final List<ChunkPos> chunks = new ArrayList<>();
    private final ChunkPosComparator comparator;
    private final int maxCommandsPerTick;
    private final boolean changedBlockOnly;
    private final ReplaceBehavior replace;
    private int sentCommandsThisTick;
    private int sentCommandsTotal;
    private int currentX;
    private int currentY;
    private int currentZ;
    private int currentIndex;
    private int boxVolume;
    private boolean boxInProgress;

    public TaskPasteSchematicSetblock(SchematicPlacement placement, LayerRange range, boolean changedBlocksOnly)
    {
        this.changedBlockOnly = changedBlocksOnly;
        this.maxCommandsPerTick = Configs.Generic.PASTE_COMMAND_LIMIT.getIntegerValue();
        this.comparator = new ChunkPosComparator();
        this.comparator.setClosestFirst(true);
        this.replace = (ReplaceBehavior) Configs.Generic.PASTE_REPLACE_BEHAVIOR.getOptionListValue();
        this.name = StringUtils.translate("litematica.gui.label.task_name.paste");

        Set<ChunkPos> touchedChunks = placement.getTouchedChunks();

        for (ChunkPos pos : touchedChunks)
        {
            int count = 0;

            for (IntBoundingBox box : placement.getBoxesWithinChunk(pos.x, pos.z).values())
            {
                box = range.getClampedBox(box);

                if (box != null)
                {
                    this.boxesInChunks.put(pos, box);
                    ++count;
                }
            }

            if (count > 0)
            {
                this.chunks.add(pos);
            }
        }

        this.sortChunkList();

        InfoHud.getInstance().addInfoHudRenderer(this, true);
        this.updateInfoHudLines();
    }

    @Override
    public boolean canExecute()
    {
        return this.boxesInChunks.isEmpty() == false && this.mc.world != null && this.mc.player != null;
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
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback false");
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

    private void sortChunkList()
    {
        if (this.chunks.size() > 0)
        {
            if (this.mc.player != null)
            {
                this.comparator.setReferencePosition(new BlockPos(this.mc.player));
                Collections.sort(this.chunks, this.comparator);
            }

            this.boxesInCurrentChunk.clear();
            this.boxesInCurrentChunk.addAll(this.boxesInChunks.get(this.chunks.get(0)));
        }
    }

    protected boolean canProcessChunk(ChunkPos pos, World worldSchematic, WorldClient worldClient)
    {
        if (worldSchematic.getChunkProvider().isChunkGeneratedAt(pos.x, pos.z) == false ||
            DataManager.getSchematicPlacementManager().hasPendingRebuildFor(pos))
        {
            return false;
        }

        // Chunk exists in the schematic world, and all the surrounding chunks are loaded in the client world, good to go
        return this.areSurroundingChunksLoaded(pos, worldClient, 1);
    }

    protected boolean processBox(ChunkPos pos, IntBoundingBox box,
            WorldSchematic worldSchematic, WorldClient worldClient, EntityPlayerSP player)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
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
            posMutable.setPos(this.currentX, this.currentY, this.currentZ);

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

                    this.sendSetBlockCommand(posMutable.getX(), posMutable.getY(), posMutable.getZ(), stateSchematicOrig, player);

                    if (++this.sentCommandsThisTick >= this.maxCommandsPerTick)
                    {
                        break;
                    }
                }
            }
        }

        if (this.currentIndex >= this.boxVolume)
        {
            this.summonEntities(box, worldSchematic, player);
            this.boxInProgress = false;

            return true;
        }

        return false;
    }

    private void summonEntities(IntBoundingBox box, WorldSchematic worldSchematic, EntityPlayerSP player)
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

                String strCommand = String.format(Locale.ROOT, "/summon %s %f %f %f", entityName, entity.posX, entity.posY, entity.posZ);
                /*
                String strCommand = String.format("/summon %s %f %f %f %s", entityName, entity.posX, entity.posY, entity.posZ, nbtString);
                System.out.printf("entity: %s\n", entity);
                System.out.printf("%s\n", strCommand);
                System.out.printf("nbt: %s\n", nbtString);
                */

                player.sendChatMessage(strCommand);
            }
        }
    }

    private void sendSetBlockCommand(int x, int y, int z, IBlockState state, EntityPlayerSP player)
    {
        Block block = state.getBlock();
        ResourceLocation rl = Block.REGISTRY.getNameForObject(block);

        if (rl == null)
        {
            return;
        }

        String blockName = rl.toString();
        String cmdName = Configs.Generic.PASTE_COMMAND_SETBLOCK.getStringValue();
        String strCommand = String.format("/%s %d %d %d %s %d", cmdName, x, y, z, blockName, block.getMetaFromState(state));

        player.sendChatMessage(strCommand);
        ++this.sentCommandsTotal;
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted_using_setblock", this.sentCommandsTotal);
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        if (this.mc.player != null)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.stop();
    }

    private void updateInfoHudLines()
    {
        List<String> hudLines = new ArrayList<>();

        String pre = GuiBase.TXT_WHITE + GuiBase.TXT_BOLD;
        String title = StringUtils.translate("litematica.gui.label.schematic_paste.missing_chunks", this.chunks.size());
        hudLines.add(String.format("%s%s%s", pre, title, GuiBase.TXT_RST));

        int maxLines = Math.min(this.chunks.size(), Configs.InfoOverlays.INFO_HUD_MAX_LINES.getIntegerValue());

        for (int i = 0; i < maxLines; ++i)
        {
            ChunkPos pos = this.chunks.get(i);
            hudLines.add(String.format("cx: %5d, cz: %5d (x: %d, z: %d)", pos.x, pos.z, pos.x << 4, pos.z << 4));
        }

        this.infoHudLines = hudLines;
    }
}
