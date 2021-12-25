package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PasteNbtBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils;

public class TaskPasteSchematicPerChunkCommand extends TaskPasteSchematicPerChunkBase
{
    protected final PasteNbtBehavior nbtBehavior;
    protected final String setBlockCommand;
    protected final Consumer<Text> gameRuleListener;
    protected final boolean ignoreBlocks;
    protected final boolean ignoreEntities;

    protected TaskPhase phase = TaskPhase.INIT;
    @Nullable protected ChunkPos currentChunk;
    @Nullable protected IntBoundingBox currentBox;
    @Nullable protected Iterator<Entity> entityIterator;
    @Nullable protected Iterator<BlockPos> positionIterator;
    protected int maxCommandsPerTick;
    protected int processedChunksThisTick;
    protected int sentCommandsThisTick;
    protected int sentCommandsTotal;
    protected int sentSetblockCommands;
    protected long gameRuleProbeTimeout;
    protected long maxGameRuleProbeTime = 2000000000L; // 2 second timeout
    protected boolean shouldEnableFeedback;

    public enum TaskPhase
    {
        INIT,
        GAMERULE_PROBE,
        WAIT_FOR_CHUNKS,
        PROCESS_BOX_BLOCKS,
        PROCESS_BOX_ENTITIES,
        FINISHED
    }

    public TaskPasteSchematicPerChunkCommand(Collection<SchematicPlacement> placements,
                                             LayerRange range,
                                             boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        this.maxCommandsPerTick = Configs.Generic.PASTE_COMMAND_LIMIT.getIntegerValue();
        this.ignoreBlocks = false;
        this.ignoreEntities = Configs.Generic.PASTE_IGNORE_ENTITIES.getBooleanValue();
        this.setBlockCommand = Configs.Generic.PASTE_COMMAND_SETBLOCK.getStringValue();
        this.nbtBehavior = (PasteNbtBehavior) Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue();
        this.gameRuleListener = this::checkGameRuleState;
    }

    @Override
    public boolean execute()
    {
        // Nothing to do
        if (this.ignoreBlocks && this.ignoreEntities)
        {
            return true;
        }

        if (this.phase == TaskPhase.INIT)
        {
            if (Configs.Generic.PASTE_DISABLE_FEEDBACK.getBooleanValue())
            {
                DataManager.addChatListener(this.gameRuleListener);
                this.mc.player.sendChatMessage("/gamerule sendCommandFeedback");
                this.gameRuleProbeTimeout = Util.getMeasuringTimeNano() + this.maxGameRuleProbeTime;
                this.phase = TaskPhase.GAMERULE_PROBE;
            }
            else
            {
                this.shouldEnableFeedback = false;
                this.phase = TaskPhase.WAIT_FOR_CHUNKS;
            }
        }

        if (this.phase == TaskPhase.GAMERULE_PROBE)
        {
            if (Util.getMeasuringTimeNano() > this.gameRuleProbeTimeout)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, 8000, "litematica.message.error.schematic_paste_failed.game_rule_probe_timeout");
                this.finished = false;
                return true;
            }

            return false;
        }

        this.sentCommandsThisTick = 0;
        this.processedChunksThisTick = 0;

        if (this.currentChunk != null &&
            this.canProcessChunk(this.currentChunk, this.schematicWorld, this.mc.world) == false)
        {
            return false;
        }

        int commandsLast = -1;
        ChunkPos lastChunk = this.currentChunk;

        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               (this.sentCommandsThisTick > commandsLast || Objects.equals(lastChunk, this.currentChunk) == false))
        {
            commandsLast = this.sentCommandsThisTick;
            lastChunk = this.currentChunk;

            if (this.phase == TaskPhase.WAIT_FOR_CHUNKS)
            {
                this.fetchNextChunk();
            }

            if (this.phase == TaskPhase.PROCESS_BOX_BLOCKS)
            {
                this.processBlocksInBox(this.currentChunk, this.currentBox);
            }

            if (this.phase == TaskPhase.PROCESS_BOX_ENTITIES)
            {
                this.processEntitiesInBox(this.currentBox);
            }

            if (this.phase == TaskPhase.FINISHED)
            {
                this.finished = true;
                return true;
            }
        }

        if (this.processedChunksThisTick > 0)
        {
            this.updateInfoHudLines();
        }

        return false;
    }

    public void checkGameRuleState(Text message)
    {
        if (message instanceof TranslatableText translatableText)
        {
            if ("commands.gamerule.query".equals(translatableText.getKey()))
            {
                this.shouldEnableFeedback = translatableText.getString().contains("true");
                this.phase = TaskPhase.WAIT_FOR_CHUNKS;

                if (this.shouldEnableFeedback)
                {
                    this.mc.player.sendChatMessage("/gamerule sendCommandFeedback false");
                }
            }
        }
    }

    protected void fetchNextChunk()
    {
        if (this.pendingChunks.isEmpty() == false)
        {
            this.sortChunkList();

            ChunkPos pos = this.pendingChunks.get(0);

            if (this.canProcessChunk(pos, this.schematicWorld, this.mc.world))
            {
                this.currentChunk = pos;
                this.startNextBox(pos);
            }
        }
        else
        {
            this.phase = TaskPhase.FINISHED;
            this.finished = true;
        }
    }

    protected void fetchNextBoxForChunk(ChunkPos pos)
    {
        List<IntBoundingBox> list = this.boxesInChunks.get(pos);

        if (list.isEmpty() == false)
        {
            this.currentBox = list.get(0);

            if (this.ignoreBlocks == false)
            {
                IntBoundingBox box = this.currentBox;
                this.positionIterator = BlockPos.iterate(box.minX, box.minY, box.minZ,
                                                         box.maxX, box.maxY, box.maxZ).iterator();
            }
        }
        else
        {
            this.currentBox = null;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        }
    }

    protected void startNextBox(ChunkPos chunkPos)
    {
        List<IntBoundingBox> list = this.boxesInChunks.get(chunkPos);

        if (list.isEmpty() == false)
        {
            this.currentBox = list.get(0);

            if (this.ignoreBlocks == false)
            {
                this.startSettingBlocks(this.currentBox);
            }
            else
            {
                this.startSummoningEntities(this.currentBox);
            }
        }
        else
        {
            this.currentBox = null;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
        }
    }

    protected void startSettingBlocks(IntBoundingBox box)
    {
        this.positionIterator = BlockPos.iterate(box.minX, box.minY, box.minZ,
                                                 box.maxX, box.maxY, box.maxZ).iterator();
        this.phase = TaskPhase.PROCESS_BOX_BLOCKS;
    }

    protected void startSummoningEntities(IntBoundingBox box)
    {
        net.minecraft.util.math.Box bb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        this.entityIterator = this.schematicWorld.getOtherEntities(null, bb, (e) -> true).iterator();
        this.phase = TaskPhase.PROCESS_BOX_ENTITIES;
    }

    protected void onFinishedProcessingBox(ChunkPos chunkPos, IntBoundingBox box)
    {
        this.boxesInChunks.remove(chunkPos, box);
        this.currentBox = null;
        this.entityIterator = null;
        this.positionIterator = null;

        if (this.boxesInChunks.get(chunkPos).isEmpty())
        {
            this.pendingChunks.remove(chunkPos);
            this.currentChunk = null;
            ++this.processedChunksThisTick;
            this.phase = TaskPhase.WAIT_FOR_CHUNKS;
            this.fetchNextChunk();
        }
        else
        {
            this.startNextBox(chunkPos);
        }
    }

    protected void processBlocksInBox(ChunkPos chunkPos, IntBoundingBox box)
    {
        ChunkSchematic schematicChunk = this.schematicWorld.getChunkProvider().getChunk(chunkPos.x, chunkPos.z);
        Chunk clientChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);

        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               this.positionIterator.hasNext())
        {
            BlockPos pos = this.positionIterator.next();
            this.pasteBlock(pos, schematicChunk, clientChunk);
        }

        if (this.positionIterator.hasNext() == false)
        {
            if (this.ignoreEntities)
            {
                this.onFinishedProcessingBox(this.currentChunk, box);
            }
            else
            {
                this.startSummoningEntities(box);
            }
        }
    }

    protected void processEntitiesInBox(IntBoundingBox box)
    {
        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               this.entityIterator.hasNext())
        {
            this.summonEntity(this.entityIterator.next());
        }

        if (this.entityIterator.hasNext() == false)
        {
            this.onFinishedProcessingBox(this.currentChunk, box);
        }
    }

    protected void pasteBlock(BlockPos pos, WorldChunk schematicChunk, Chunk clientChunk)
    {
        BlockState stateSchematic = schematicChunk.getBlockState(pos);
        BlockState stateClient = clientChunk.getBlockState(pos);

        if (stateSchematic.isAir() == false || stateClient.isAir() == false)
        {
            if (this.changedBlockOnly == false || stateClient != stateSchematic)
            {
                if ((this.replace == ReplaceBehavior.NONE && stateClient.isAir() == false) ||
                    (this.replace == ReplaceBehavior.WITH_NON_AIR && stateSchematic.isAir()))
                {
                    return;
                }

                ClientPlayerEntity player = this.mc.player;
                PasteNbtBehavior nbtBehavior = this.nbtBehavior;
                BlockEntity be = schematicChunk.getBlockEntity(pos);

                if (be != null && nbtBehavior != PasteNbtBehavior.NONE)
                {
                    World schematicWorld = schematicChunk.getWorld();
                    ClientWorld clientWorld = this.mc.world;

                    if (nbtBehavior == PasteNbtBehavior.PLACE_MODIFY)
                    {
                        this.setDataViaDataModify(pos, stateSchematic, be, schematicWorld, clientWorld, player);
                    }
                    else if (nbtBehavior == PasteNbtBehavior.PLACE_CLONE)
                    {
                        this.placeBlockViaClone(pos, stateSchematic, be, schematicWorld, clientWorld, player);
                    }
                    else if (nbtBehavior == PasteNbtBehavior.TELEPORT_PLACE)
                    {
                        this.placeBlockDirectly(pos, stateSchematic, be, schematicWorld, clientWorld, player);
                    }
                }
                else
                {
                    this.sendSetBlockCommand(pos.getX(), pos.getY(), pos.getZ(), stateSchematic, player);
                }
            }
        }
    }

    protected void summonEntity(Entity entity)
    {
        String id = EntityUtils.getEntityId(entity);

        if (id != null)
        {
            // TODO add a config for the summon command
            String command = String.format(Locale.ROOT, "summon %s %f %f %f", id, entity.getX(), entity.getY(), entity.getZ());

            if (entity instanceof ItemFrameEntity itemFrame)
            {
                command = this.getSummonCommandForItemFrame(itemFrame, command);
            }

            this.sendCommand(command, this.mc.player);
        }
    }

    protected String getSummonCommandForItemFrame(ItemFrameEntity itemFrame, String originalCommand)
    {
        ItemStack stack = itemFrame.getHeldItemStack();

        if (stack.isEmpty() == false)
        {
            Identifier itemId = Registry.ITEM.getId(stack.getItem());
            int facingId = itemFrame.getHorizontalFacing().getId();
            String nbtStr = String.format(" {Facing:%db,Item:{id:\"%s\",Count:1b}}", facingId, itemId);
            NbtCompound tag = stack.getNbt();

            if (tag != null)
            {
                String itemNbt = tag.toString();
                String tmp = String.format(" {Facing:%db,Item:{id:\"%s\",Count:1b,tag:%s}}",
                                           facingId, itemId, itemNbt);

                if (originalCommand.length() + tmp.length() < 255)
                {
                    nbtStr = tmp;
                }
            }

            return originalCommand + nbtStr;
        }

        return originalCommand;
    }

    protected void sendSetBlockCommand(int x, int y, int z, BlockState state, ClientPlayerEntity player)
    {
        String cmdName = this.setBlockCommand;
        String blockString = BlockArgumentParser.stringifyBlockState(state);
        String strCommand = String.format("%s %d %d %d %s", cmdName, x, y, z, blockString);

        this.sendCommand(strCommand, player);
        ++this.sentSetblockCommands;
    }

    protected void setDataViaDataModify(BlockPos pos, BlockState state, BlockEntity be,
                                        World schematicWorld, ClientWorld clientWorld, ClientPlayerEntity player)
    {
        BlockPos placementPos = findEmptyNearbyPosition(clientWorld, player.getBlockPos(), 3);

        if (placementPos != null && this.preparePickedStack(pos, state, be, schematicWorld, player))
        {
            Vec3d posVec = new Vec3d(placementPos.getX() + 0.5, placementPos.getY() + 1.0, placementPos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(posVec, Direction.UP, placementPos, false);

            this.mc.interactionManager.interactBlock(player, clientWorld, Hand.OFF_HAND, hitResult);
            this.sendSetBlockCommand(pos.getX(), pos.getY(), pos.getZ(), state, player);

            try
            {
                Set<String> keys = new HashSet<>(be.createNbt().getKeys());
                keys.remove("id");
                keys.remove("x");
                keys.remove("y");
                keys.remove("z");

                for (String key : keys)
                {
                    String command = String.format("data modify block %d %d %d %s set from block %d %d %d %s",
                                                   pos.getX(), pos.getY(), pos.getZ(), key,
                                                   placementPos.getX(), placementPos.getY(), placementPos.getZ(), key);
                    this.sendCommand(command, player);
                }
            }
            catch (Exception ignore) {}

            String cmdName = this.setBlockCommand;
            String command = String.format("%s %d %d %d air", cmdName, placementPos.getX(), placementPos.getY(), placementPos.getZ());
            this.sendCommand(command, player);
        }
    }

    protected void placeBlockViaClone(BlockPos pos, BlockState state, BlockEntity be,
                                      World schematicWorld, ClientWorld clientWorld, ClientPlayerEntity player)
    {
        BlockPos placementPos = findEmptyNearbyPosition(clientWorld, player.getBlockPos(), 3);

        if (placementPos != null && this.preparePickedStack(pos, state, be, schematicWorld, player))
        {
            Vec3d posVec = new Vec3d(placementPos.getX() + 0.5, placementPos.getY() + 1.0, placementPos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(posVec, Direction.UP, placementPos, false);
            this.mc.interactionManager.interactBlock(player, clientWorld, Hand.OFF_HAND, hitResult);

            /*
            {
                String command = String.format("data get block %d %d %d", placementPos.getX(), placementPos.getY(), placementPos.getZ());
                this.sendCommand(command, player);
            }
            */

            // TODO add a config for the clone command
            String command = String.format("clone %d %d %d %d %d %d %d %d %d",
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           pos.getX(), pos.getY(), pos.getZ());
            this.sendCommand(command, player);

            String cmdName = this.setBlockCommand;
            command = String.format("%s %d %d %d air", cmdName, placementPos.getX(), placementPos.getY(), placementPos.getZ());
            this.sendCommand(command, player);
        }
    }

    // FIXME this method does not work, probably because of the player being too far and the teleport command getting executed later(?)
    protected void placeBlockDirectly(BlockPos pos, BlockState state, BlockEntity be,
                                      World schematicWorld, ClientWorld clientWorld, ClientPlayerEntity player)
    {
        if (this.preparePickedStack(pos, state, be, schematicWorld, player))
        {
            player.setPos(pos.getX(), pos.getY() + 2, pos.getZ());

            String command = String.format("tp @p %d %d %d", pos.getX(), pos.getY() + 2, pos.getZ());
            this.sendCommand(command, player);

            Vec3d posVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(posVec, Direction.UP, pos, false);
            this.mc.interactionManager.interactBlock(player, clientWorld, Hand.OFF_HAND, hitResult);
        }
    }

    protected void sendCommand(String command, ClientPlayerEntity player)
    {
        this.sendCommandToServer(command, player);
        ++this.sentCommandsThisTick;
        ++this.sentCommandsTotal;
    }

    protected void sendCommandToServer(String command, ClientPlayerEntity player)
    {
        if (command.length() > 0 && command.charAt(0) != '/')
        {
            player.sendChatMessage("/" + command);
        }
        else
        {
            player.sendChatMessage(command);
        }
    }

    @Nullable
    public static BlockPos findEmptyNearbyPosition(World world, BlockPos centerPos, int radius)
    {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos.Mutable sidePos = new BlockPos.Mutable();

        for (int y = centerPos.getY(); y <= centerPos.getY() + radius; ++y)
        {
            for (int z = centerPos.getZ() - radius; z <= centerPos.getZ() + radius; ++z)
            {
                for (int x = centerPos.getX() - radius; x <= centerPos.getX() + radius; ++x)
                {
                    pos.set(x, y, z);

                    if (isPositionAndSidesEmpty(world, pos, sidePos))
                    {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    public static boolean isPositionAndSidesEmpty(World world, BlockPos.Mutable centerPos, BlockPos.Mutable pos)
    {
        if (world.isAir(centerPos) == false)
        {
            return false;
        }

        for (Direction side : PositionUtils.ALL_DIRECTIONS)
        {
            if (world.isAir(pos.set(centerPos, side)) == false)
            {
                return false;
            }
        }

        return true;
    }

    protected boolean preparePickedStack(BlockPos pos, BlockState state, BlockEntity be, World world, ClientPlayerEntity player)
    {
        ItemStack stack = state.getBlock().getPickStack(world, pos, state);

        if (stack.isEmpty() == false)
        {
            addBlockEntityNbt(stack, be);
            player.getInventory().offHand.set(0, stack);
            this.mc.interactionManager.clickCreativeStack(stack, 45);
            return true;
        }

        return false;
    }

    public static void addBlockEntityNbt(ItemStack stack, BlockEntity be)
    {
        NbtCompound tag = be.createNbt();

        if (stack.getItem() instanceof SkullItem && tag.contains("SkullOwner"))
        {
            NbtCompound ownerTag = tag.getCompound("SkullOwner");
            stack.getOrCreateNbt().put("SkullOwner", ownerTag);
        }
        else
        {
            stack.setSubNbt("BlockEntityTag", tag);
        }
    }

    @Override
    public void stop()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                InfoUtils.showGuiOrActionBarMessage(MessageType.SUCCESS, "litematica.message.schematic_pasted_using_setblock", this.sentSetblockCommands);
            }
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        if (this.mc.player != null && this.shouldEnableFeedback)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        DataManager.removeChatListener(this.gameRuleListener);
        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.stop();
    }
}
