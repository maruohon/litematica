package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PasteNbtBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.ChunkSchematic;

public class TaskPasteSchematicPerChunkCommand extends TaskPasteSchematicPerChunkBase
{
    protected final Queue<String> queuedCommands = Queues.newArrayDeque();
    protected final Long2LongOpenHashMap placedPositionTimestamps = new Long2LongOpenHashMap();
    protected final LongArrayList fillVolumes = new LongArrayList();
    protected final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
    protected final PasteNbtBehavior nbtBehavior;
    protected final String cloneCommand;
    protected final String fillCommand;
    protected final String setBlockCommand;
    protected final String summonCommand;
    protected final int maxBoxVolume;
    protected final boolean useFillCommand;
    protected final boolean useWorldEdit;
    protected int[][][] workArr;
    protected int sentFillCommands;
    protected int sentSetblockCommands;

    public TaskPasteSchematicPerChunkCommand(Collection<SchematicPlacement> placements,
                                             LayerRange range,
                                             boolean changedBlocksOnly)
    {
        super(placements, range, changedBlocksOnly);

        this.maxCommandsPerTick = Configs.Generic.COMMAND_LIMIT.getIntegerValue();
        this.maxBoxVolume = Configs.Generic.COMMAND_FILL_MAX_VOLUME.getIntegerValue();
        this.cloneCommand = Configs.Generic.COMMAND_NAME_CLONE.getStringValue();
        this.fillCommand = Configs.Generic.COMMAND_NAME_FILL.getStringValue();
        this.setBlockCommand = Configs.Generic.COMMAND_NAME_SETBLOCK.getStringValue();
        this.summonCommand = Configs.Generic.COMMAND_NAME_SUMMON.getStringValue();
        this.useFillCommand = Configs.Generic.PASTE_USE_FILL_COMMAND.getBooleanValue();
        this.useWorldEdit = Configs.Generic.COMMAND_USE_WORLDEDIT.getBooleanValue();
        this.nbtBehavior = (PasteNbtBehavior) Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue();

        if (this.useFillCommand)
        {
            this.processBoxBlocksTask = this::processBlocksInCurrentBoxUsingFill;
        }
        else
        {
            this.processBoxBlocksTask = this::processBlocksInCurrentBoxUsingSetBlockOnly;
        }

        this.processBoxEntitiesTask = this::processEntitiesInCurrentBox;
    }

    @Override
    public boolean execute()
    {
        // Nothing to do
        if (this.ignoreBlocks && this.ignoreEntities)
        {
            return true;
        }

        return this.executeMultiPhase();
    }

    @Override
    public void init()
    {
        super.init();

        if (this.useWorldEdit && this.mc.player != null)
        {
            this.mc.player.sendChatMessage("//perf neighbors off");
        }
    }

    @Override
    protected void onNextChunkFetched(ChunkPos pos)
    {
        this.startNextBox(pos);
    }

    @Override
    protected void onStartNextBox(IntBoundingBox box)
    {
        if (this.ignoreBlocks == false)
        {
            this.prepareSettingBlocks(box);
        }
        else
        {
            this.prepareSummoningEntities(box);
        }
    }

    protected void prepareSettingBlocks(IntBoundingBox box)
    {
        if (this.useFillCommand)
        {
            this.generateFillVolumes(box);
        }
        else
        {
            this.positionIterator = BlockPos.iterate(box.minX, box.minY, box.minZ,
                                                     box.maxX, box.maxY, box.maxZ).iterator();
        }

        this.phase = TaskPhase.PROCESS_BOX_BLOCKS;
    }

    protected void prepareSummoningEntities(IntBoundingBox box)
    {
        net.minecraft.util.math.Box bb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        this.entityIterator = this.schematicWorld.getOtherEntities(null, bb, (e) -> true).iterator();
        this.phase = TaskPhase.PROCESS_BOX_ENTITIES;
    }

    protected void sendQueuedCommands()
    {
        while (this.sentCommandsThisTick < this.maxCommandsPerTick && this.queuedCommands.isEmpty() == false)
        {
            this.sendCommand(this.queuedCommands.poll(), this.mc.player);
        }
    }

    protected void sendCommand(String cmd)
    {
        this.sendCommand(cmd, this.mc.player);
    }

    protected void processBlocksInCurrentBoxUsingSetBlockOnly()
    {
        ChunkPos chunkPos = this.currentChunkPos;
        ChunkSchematic schematicChunk = this.schematicWorld.getChunkProvider().getChunk(chunkPos.x, chunkPos.z);
        Chunk clientChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);
        boolean ignoreLimit = Configs.Generic.PASTE_IGNORE_CMD_LIMIT.getBooleanValue();

        while (this.positionIterator.hasNext() &&
               this.queuedCommands.size() < this.maxCommandsPerTick &&
               (ignoreLimit == false || this.sentCommandsThisTick < this.maxCommandsPerTick))
        {
            BlockPos pos = this.positionIterator.next();
            this.pasteBlock(pos, schematicChunk, clientChunk, ignoreLimit);
        }

        this.sendQueuedCommands();

        if (this.positionIterator.hasNext() == false && this.queuedCommands.isEmpty())
        {
            if (this.ignoreEntities)
            {
                this.onFinishedProcessingBox(this.currentChunkPos, this.currentBox);
            }
            else
            {
                this.prepareSummoningEntities(this.currentBox);
            }
        }
    }

    protected void processBlocksInCurrentBoxUsingFill()
    {
        ChunkPos chunkPos = this.currentChunkPos;
        final int baseX = chunkPos.x << 4;
        final int baseZ = chunkPos.z << 4;
        ChunkSchematic schematicChunk = this.schematicWorld.getChunkProvider().getChunk(chunkPos.x, chunkPos.z);
        Chunk clientChunk = this.mc.world.getChunk(chunkPos.x, chunkPos.z);

        while (this.fillVolumes.isEmpty() == false && this.queuedCommands.size() < this.maxCommandsPerTick)
        {
            int index = this.fillVolumes.size() - 1;
            long encodedValue = this.fillVolumes.removeLong(index);
            //System.out.printf("filling encoded: 0x%016X\n", encodedValue);
            this.fillVolume(encodedValue, baseX, baseZ, schematicChunk, clientChunk);
        }

        this.sendQueuedCommands();

        if (this.fillVolumes.isEmpty() && this.queuedCommands.isEmpty())
        {
            if (this.ignoreEntities)
            {
                this.onFinishedProcessingBox(this.currentChunkPos, this.currentBox);
            }
            else
            {
                this.prepareSummoningEntities(this.currentBox);
            }
        }
    }

    protected void processEntitiesInCurrentBox()
    {
        while (this.entityIterator.hasNext() && this.queuedCommands.size() < this.maxCommandsPerTick)
        {
            this.summonEntity(this.entityIterator.next());
        }

        this.sendQueuedCommands();

        if (this.entityIterator.hasNext() == false && this.queuedCommands.isEmpty())
        {
            this.onFinishedProcessingBox(this.currentChunkPos, this.currentBox);
        }
    }

    protected void pasteBlock(BlockPos pos, WorldChunk schematicChunk, Chunk clientChunk, boolean ignoreLimit)
    {
        BlockState stateSchematic = schematicChunk.getBlockState(pos);
        BlockState stateClient = clientChunk.getBlockState(pos);

        if (this.shouldSetBlock(stateSchematic, stateClient))
        {
            PasteNbtBehavior nbtBehavior = this.nbtBehavior;
            BlockEntity be = schematicChunk.getBlockEntity(pos);

            if (be != null && nbtBehavior != PasteNbtBehavior.NONE)
            {
                Consumer<String> commandHandler = ignoreLimit ? this::sendCommand : this.queuedCommands::offer;
                World schematicWorld = schematicChunk.getWorld();

                if (nbtBehavior == PasteNbtBehavior.PLACE_MODIFY)
                {
                    this.setDataViaDataModify(pos, stateSchematic, be, schematicWorld, this.mc.world, commandHandler);
                }
                else if (nbtBehavior == PasteNbtBehavior.PLACE_CLONE)
                {
                    this.placeBlockViaClone(pos, stateSchematic, be, schematicWorld, this.mc.world, commandHandler);
                }
            }
            else
            {
                this.queueSetBlockCommand(pos.getX(), pos.getY(), pos.getZ(), stateSchematic);
            }
        }
    }

    protected boolean shouldSetBlock(BlockState stateSchematic, BlockState stateClient)
    {
        if (stateSchematic.hasBlockEntity() && Configs.Generic.PASTE_IGNORE_BE_ENTIRELY.getBooleanValue())
        {
            return false;
        }

        if ((stateSchematic.isAir() && stateClient.isAir()) ||
            (this.changedBlockOnly && stateClient == stateSchematic))
        {
            return false;
        }

        return (this.replace != ReplaceBehavior.NONE || stateClient.isAir()) &&
                (this.replace != ReplaceBehavior.WITH_NON_AIR || stateSchematic.isAir() == false);
    }

    protected void summonEntity(Entity entity)
    {
        String id = EntityUtils.getEntityId(entity);

        if (id != null)
        {
            String command = String.format(Locale.ROOT, "%s %s %f %f %f",
                                           this.summonCommand, id, entity.getX(), entity.getY(), entity.getZ());

            if (entity instanceof ItemFrameEntity itemFrame)
            {
                command = this.getSummonCommandForItemFrame(itemFrame, command);
            }

            this.queuedCommands.offer(command);
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

    protected void queueSetBlockCommand(int x, int y, int z, BlockState state)
    {
        this.queueSetBlockCommand(x, y, z, state, this.queuedCommands::offer);
    }

    protected void queueSetBlockCommand(int x, int y, int z, BlockState state, Consumer<String> commandHandler)
    {
        String blockString = BlockArgumentParser.stringifyBlockState(state);

        if (this.useWorldEdit)
        {
            commandHandler.accept(String.format("//pos1 %d,%d,%d", x, y, z));
            commandHandler.accept(String.format("//pos2 %d,%d,%d", x, y, z));
            commandHandler.accept("//set " + blockString);
        }
        else
        {
            String cmdName = this.setBlockCommand;
            commandHandler.accept(String.format("%s %d %d %d %s", cmdName, x, y, z, blockString));
        }

        ++this.sentSetblockCommands;
    }

    protected void pasteVolume(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state)
    {
        final int minX = Math.min(x1, x2);
        final int minY = Math.min(y1, y2);
        final int minZ = Math.min(z1, z2);
        final int maxX = Math.max(x1, x2);
        final int maxY = Math.max(y1, y2);
        final int maxZ = Math.max(z1, z2);
        final int singleLayerVolume = (maxX - minX + 1) * (maxZ - minZ + 1);
        final int totalVolume = singleLayerVolume * (maxY - minY + 1);

        if (totalVolume <= this.maxBoxVolume || this.useWorldEdit)
        {
            this.queueFillCommandForBox(minX, minY, minZ, maxX, maxY, maxZ, state);
        }
        else
        {
            int singleBoxHeight = this.maxBoxVolume / singleLayerVolume;

            if (singleBoxHeight < 1)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Error: Calculated single box height was less than 1 block");
                return;
            }

            for (int y = minY; y <= maxY; y += singleBoxHeight)
            {
                int boxMaxY = Math.min(y + singleBoxHeight - 1, maxY);
                this.queueFillCommandForBox(minX, y, minZ, maxX, boxMaxY, maxZ, state);
            }
        }
    }

    protected void queueFillCommandForBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state)
    {
        String blockString = BlockArgumentParser.stringifyBlockState(state);

        if (this.useWorldEdit)
        {
            this.queuedCommands.offer(String.format("//pos1 %d,%d,%d", minX, minY, minZ));
            this.queuedCommands.offer(String.format("//pos2 %d,%d,%d", maxX, maxY, maxZ));
            this.queuedCommands.offer("//set " + blockString);
        }
        else
        {
            final String cmdName = this.fillCommand;
            String fillCommand = String.format("%s %d %d %d %d %d %d %s",
                                               cmdName, minX, minY, minZ, maxX, maxY, maxZ, blockString);

            if (this.replace == ReplaceBehavior.NONE ||
                (this.replace == ReplaceBehavior.WITH_NON_AIR && state.isAir()))
            {
                fillCommand += " replace air";
            }

            this.queuedCommands.offer(fillCommand);
        }

        ++this.sentFillCommands;
    }

    protected void setDataViaDataModify(BlockPos pos, BlockState state, BlockEntity be,
                                        World schematicWorld, ClientWorld clientWorld,
                                        Consumer<String> commandHandler)
    {
        BlockPos placementPos = this.placeNbtPickedBlock(pos, state, be, schematicWorld, clientWorld);

        if (placementPos != null)
        {
            this.queueSetBlockCommand(pos.getX(), pos.getY(), pos.getZ(), state, commandHandler);

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
                    commandHandler.accept(command);
                }
            }
            catch (Exception ignore) {}

            String cmdName = this.setBlockCommand;
            String command = String.format("%s %d %d %d air",
                                           cmdName, placementPos.getX(), placementPos.getY(), placementPos.getZ());
            commandHandler.accept(command);
        }
    }

    protected void placeBlockViaClone(BlockPos pos, BlockState state, BlockEntity be,
                                      World schematicWorld, ClientWorld clientWorld,
                                      Consumer<String> commandHandler)
    {
        BlockPos placementPos = this.placeNbtPickedBlock(pos, state, be, schematicWorld, clientWorld);

        if (placementPos != null)
        {
            String command = String.format("%s %d %d %d %d %d %d %d %d %d",
                                           this.cloneCommand,
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           pos.getX(), pos.getY(), pos.getZ());
            commandHandler.accept(command);

            String cmdName = this.setBlockCommand;
            command = String.format("%s %d %d %d air",
                                    cmdName, placementPos.getX(), placementPos.getY(), placementPos.getZ());
            commandHandler.accept(command);
        }
    }

    @Nullable
    protected BlockPos placeNbtPickedBlock(BlockPos pos, BlockState state, BlockEntity be,
                                           World schematicWorld, ClientWorld clientWorld)
    {
        double reach = this.mc.interactionManager.getReachDistance();
        BlockPos placementPos = this.findEmptyNearbyPosition(clientWorld, this.mc.player.getPos(), 4, reach);

        if (placementPos != null && preparePickedStack(pos, state, be, schematicWorld, this.mc))
        {
            Vec3d posVec = new Vec3d(placementPos.getX() + 0.5, placementPos.getY() + 0.5, placementPos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(posVec, Direction.UP, placementPos, true);

            this.mc.interactionManager.interactBlock(this.mc.player, clientWorld, Hand.OFF_HAND, hitResult);
            this.placedPositionTimestamps.put(placementPos.asLong(), System.nanoTime());

            return placementPos;
        }

        return null;
    }

    protected void fillVolume(long encodedValue, int baseX, int baseZ,
                              ChunkSchematic schematicChunk, Chunk clientChunk)
    {
        int startPos = (int) encodedValue;
        int packedOffset = getPackedSize(encodedValue);
        int startX = unpackX(startPos) + baseX;
        int startY = unpackY(startPos);
        int startZ = unpackZ(startPos) + baseZ;
        int endOffsetX = unpackX(packedOffset);
        int endOffsetY = unpackY(packedOffset);
        int endOffsetZ = unpackZ(packedOffset);

        this.mutablePos.set(startX, startY, startZ);

        if ((endOffsetX > 0 || endOffsetY > 0 || endOffsetZ > 0) ||
            Configs.Generic.PASTE_ALWAYS_USE_FILL.getBooleanValue())
        {
            int endX = startX + endOffsetX;
            int endY = startY + endOffsetY;
            int endZ = startZ + endOffsetZ;
            BlockState state = schematicChunk.getBlockState(this.mutablePos);

            //System.out.printf("fill @ [%d %d %d] -> [%d %d %d] (%d x %d x %d) %s\n", startX, startY, startZ, endX, endY, endZ, endOffsetX + 1, endOffsetY + 1, endOffsetZ + 1, state);
            this.pasteVolume(startX, startY, startZ, endX, endY, endZ, state);
        }
        else
        {
            //System.out.printf("fill -> setblock @ [%d %d %d] %s\n", startX, startY, startZ, schematicChunk.getBlockState(this.mutablePos));
            this.pasteBlock(this.mutablePos, schematicChunk, clientChunk, false);
        }
    }

    protected void generateFillVolumes(IntBoundingBox box)
    {
        ChunkSchematic chunk = this.schematicWorld.getChunkProvider().getChunk(box.minX >> 4, box.minZ >> 4);
        boolean ignoreBeFromFill = Configs.Generic.PASTE_IGNORE_BE_IN_FILL.getBooleanValue() &&
                                   Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue() != PasteNbtBehavior.NONE;
        

        this.fillVolumes.clear();

        if (this.workArr == null)
        {
            int height = this.world.getHeight();
            this.workArr = new int[16][height][16];
        }

        this.generateStrips(this.workArr, Direction.EAST, box, chunk, ignoreBeFromFill);
        this.combineStripsToLayers(this.workArr, Direction.EAST, Direction.SOUTH, Direction.UP,
                                   box, chunk, this.fillVolumes, ignoreBeFromFill);
        Collections.reverse(this.fillVolumes);
    }

    protected int getBlockStripLength(BlockPos.Mutable pos,
                                      Direction direction,
                                      int maxLength,
                                      BlockState firstState,
                                      Chunk chunk)
    {
        int length = 1;

        while (length < maxLength)
        {
            pos.move(direction);
            BlockState state = chunk.getBlockState(pos);

            if (state != firstState)
            {
                break;
            }

            ++length;
        }

        return length;
    }

    protected void generateStrips(int[][][] workArr,
                                  Direction stripDirection,
                                  IntBoundingBox box,
                                  ChunkSchematic chunk,
                                  boolean ignoreBeFromFill)
    {
        boolean ignoreBeEntirely = Configs.Generic.PASTE_IGNORE_BE_ENTIRELY.getBooleanValue();
        BlockPos.Mutable mutablePos = this.mutablePos;
        ReplaceBehavior replace = this.replace;
        final int startX = box.minX & 0xF;
        final int startZ = box.minZ & 0xF;
        final int endX = box.maxX & 0xF;
        final int endZ = box.maxZ & 0xF;
        final int worldMinY = chunk.getBottomY();

        for (int y = box.minY; y <= box.maxY; ++y)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int x = startX; x <= endX; ++x)
                {
                    mutablePos.set(x, y, z);
                    BlockState state = chunk.getBlockState(mutablePos);

                    if (state.isAir() == false || replace == ReplaceBehavior.ALL)
                    {
                        if (state.hasBlockEntity())
                        {
                            if (ignoreBeFromFill)
                            {
                                workArr[x][y - worldMinY][z] = 1;
                                continue;
                            }
                            else if (ignoreBeEntirely)
                            {
                                continue;
                            }
                        }

                        int length = this.getBlockStripLength(mutablePos, stripDirection, endX - x + 1, state, chunk);
                        workArr[x][y - worldMinY][z] = length;
                        //System.out.printf("strip @ [%d %d %d] %d x %s\n", x, y, z, length, state);
                        x += length - 1;
                    }
                }
            }
        }
    }

    // TODO this method should be cleaned up and split up to smaller methods,
    // and the iteration order would need to be made adjustable for the direction
    // arguments to make sense and to work in other combinations.
    protected void combineStripsToLayers(int[][][] workArr,
                                         Direction stripDirection,
                                         Direction stripCombineDirection,
                                         Direction layerCombineDirection,
                                         IntBoundingBox box,
                                         ChunkSchematic chunk,
                                         LongArrayList volumesOut,
                                         boolean ignoreBe)
    {
        BlockPos.Mutable mutablePos = this.mutablePos;
        final int sdOffX = stripDirection.getOffsetX();
        final int sdOffY = stripDirection.getOffsetY();
        final int sdOffZ = stripDirection.getOffsetZ();
        final int scOffX = stripCombineDirection.getOffsetX();
        final int scOffY = stripCombineDirection.getOffsetY();
        final int scOffZ = stripCombineDirection.getOffsetZ();
        final int lcOffX = layerCombineDirection.getOffsetX();
        final int lcOffY = layerCombineDirection.getOffsetY();
        final int lcOffZ = layerCombineDirection.getOffsetZ();
        final int startX = box.minX & 0xF;
        final int startZ = box.minZ & 0xF;
        final int endX = box.maxX & 0xF;
        final int endZ = box.maxZ & 0xF;
        final int worldMinY = chunk.getBottomY();

        for (int y = box.minY; y <= box.maxY; ++y)
        {
            for (int x = startX; x <= endX; ++x)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    int length = workArr[x][y - worldMinY][z];

                    if (length > 0)
                    {
                        int nextX = x + scOffX;
                        int nextY = y + scOffY;
                        int nextZ = z + scOffZ;
                        int stripCount = 1;

                        mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(mutablePos);

                        if (ignoreBe == false || state.hasBlockEntity() == false)
                        {
                            // Find identical adjacent strips, and set their data in the array to zero,
                            // since they are being combined into one layer starting from the first position.
                            while (nextX <= 15 && nextY <= box.maxY && nextZ <= 15 &&
                                   workArr[nextX][nextY - worldMinY][nextZ] == length &&
                                   chunk.getBlockState(mutablePos.set(nextX, nextY, nextZ)) == state)
                            {
                                ++stripCount;
                                workArr[nextX][nextY - worldMinY][nextZ] = 0;
                                nextX += scOffX;
                                nextY += scOffY;
                                nextZ += scOffZ;
                            }
                        }

                        // Encode the first two dimensions of the volume (at this point a layer).
                        // Note: At this point the range is 1...16 so that it can be distinguished from "no data" = 0
                        int packedX = sdOffX * length + scOffX * stripCount;
                        int packedY = sdOffY * length + scOffY * stripCount;
                        int packedZ = sdOffZ * length + scOffZ * stripCount;
                        int packedSize = packCoordinate5bit(packedX, packedY, packedZ);

                        //System.out.printf("layer @ [%d %d %d] len: %d x count: %d %s\n", x, y, z, length, stripCount, state);
                        workArr[x][y - worldMinY][z] = packedSize;

                        // Skip the already handled/combined strips
                        if (stripCount > 1)
                        {
                            int extraStrips = stripCount - 1;
                            x += scOffX * extraStrips;
                            y += scOffY * extraStrips;
                            z += scOffZ * extraStrips;
                        }
                    }
                }
            }
        }

        for (int x = startX; x <= endX; ++x)
        {
            for (int z = startZ; z <= endZ; ++z)
            {
                for (int y = box.minY; y <= box.maxY; ++y)
                {
                    int packedSize = workArr[x][y - worldMinY][z];

                    if (packedSize != 0)
                    {
                        int nextX = x + lcOffX;
                        int nextY = y + lcOffY;
                        int nextZ = z + lcOffZ;
                        int layerCount = 1;

                        mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(mutablePos);

                        if (ignoreBe == false || state.hasBlockEntity() == false)
                        {
                            // Find identical adjacent layers
                            while (nextX <= 15 && nextY <= box.maxY && nextZ <= 15 &&
                                   workArr[nextX][nextY - worldMinY][nextZ] == packedSize &&
                                   chunk.getBlockState(mutablePos.set(nextX, nextY, nextZ)) == state)
                            {
                                ++layerCount;
                                workArr[nextX][nextY - worldMinY][nextZ] = 0;
                                nextX += lcOffX;
                                nextY += lcOffY;
                                nextZ += lcOffZ;
                            }
                        }

                        // Add the layer thickness, and change the encoding from 1...16 to 0...15
                        // All the axes here will have values of at least 1 before this -1.
                        int volumeEndOffsetX = lcOffX * layerCount + unpackX5bit(packedSize) - 1;
                        int volumeEndOffsetY = lcOffY * layerCount + unpackY5bit(packedSize) - 1;
                        int volumeEndOffsetZ = lcOffZ * layerCount + unpackZ5bit(packedSize) - 1;
                        int packedVolumeEndOffset = packCoordinate(volumeEndOffsetX, volumeEndOffsetY, volumeEndOffsetZ);

                        //System.out.printf("volume @ [%d %d %d] size: %d x %d x %d %s\n", x, y, z, volumeEndOffsetX, volumeEndOffsetY, volumeEndOffsetZ, state);
                        long encodedValue = ((long) packedVolumeEndOffset << 32L) | (packCoordinate(x, y, z) & 0xFFFFFFFFL);
                        volumesOut.add(encodedValue);

                        // Always also clear the array for the next use
                        workArr[x][y - worldMinY][z] = 0;

                        // Skip the already handled/combined strips
                        if (layerCount > 1)
                        {
                            int extraLayers = layerCount - 1;
                            x += lcOffX * extraLayers;
                            y += lcOffY * extraLayers;
                            z += lcOffZ * extraLayers;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onStop()
    {
        if (this.finished)
        {
            if (this.printCompletionMessage)
            {
                if (this.useWorldEdit)
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.schematic_pasted_using_world_edit", this.sentSetblockCommands + this.sentFillCommands);
                }
                else if (this.useFillCommand)
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.schematic_pasted_using_fill_and_setblock", this.sentFillCommands, this.sentSetblockCommands);
                }
                else
                {
                    InfoUtils.showGuiOrActionBarMessage(MessageType.INFO, "litematica.message.schematic_pasted_using_setblock", this.sentSetblockCommands);
                }
            }
        }
        else
        {
            InfoUtils.showGuiOrActionBarMessage(MessageType.ERROR, "litematica.message.error.schematic_paste_failed");
        }

        if (this.useWorldEdit)
        {
            this.mc.player.sendChatMessage("//perf neighbors on");
        }

        if (this.mc.player != null && this.shouldEnableFeedback)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        DataManager.removeChatListener(this.gameRuleListener);
        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.onStop();
    }

    protected static int unpackX(int value)
    {
        return value & 0xF;
    }

    protected static int unpackY(int value)
    {
        return (value >> 8);
    }

    protected static int unpackZ(int value)
    {
        return (value >> 4) & 0xF;
    }

    protected static int packCoordinate(int x, int y, int z)
    {
        return (y << 8) | ((z & 0xF) << 4) | (x & 0xF);
    }

    protected static int unpackX5bit(int value)
    {
        return value & 0x1F;
    }

    protected static int unpackY5bit(int value)
    {
        return (value >> 10);
    }

    protected static int unpackZ5bit(int value)
    {
        return (value >> 5) & 0x1F;
    }

    protected static int packCoordinate5bit(int x, int y, int z)
    {
        return (y << 10) | ((z & 0x1F) << 5) | (x & 0x1F);
    }

    protected static int getPackedSize(long fullPackedValue)
    {
        return (int) (fullPackedValue >> 32L);
    }


    @Nullable
    public BlockPos findEmptyNearbyPosition(World world, Vec3d centerPos, int radius, double reachDistance)
    {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos.Mutable sidePos = new BlockPos.Mutable();
        long currentTime = System.nanoTime();
        long timeout = 2000000000L; // 2 second timeout before trying to place again to the same position
        double squaredReach = reachDistance * reachDistance;
        int radiusY = Math.min(radius, 2);

        for (double y = centerPos.getY() - radiusY; y <= centerPos.getY() + radiusY; ++y)
        {
            for (double z = centerPos.getZ() - radius; z <= centerPos.getZ() + radius; ++z)
            {
                for (double x = centerPos.getX() - radius; x <= centerPos.getX() + radius; ++x)
                {
                    // Don't try to place if block is too far (server rejects it)
                    if (centerPos.squaredDistanceTo(x, y, z) > squaredReach)
                    {
                        continue;
                    }

                    // Don't try to place a block intersecting the player
                    if (MathHelper.absFloor(centerPos.getX() - x) < 2 &&
                        MathHelper.absFloor(centerPos.getZ() - z) < 2 &&
                        y >= centerPos.getY() - 2 && y <= centerPos.getY() + 2)
                    {
                        continue;
                    }

                    pos.set(x, y, z);
                    long posLong = pos.asLong();

                    if (this.placedPositionTimestamps.containsKey(posLong) &&
                        currentTime - this.placedPositionTimestamps.get(posLong) < timeout)
                    {
                        continue;
                    }

                    if (isPositionAndSidesEmpty(world, pos, sidePos))
                    {
                        return pos.toImmutable();
                    }
                }
            }
        }

        return null;
    }

    public static boolean isPositionAndSidesEmpty(World world, BlockPos centerPos, BlockPos.Mutable pos)
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

    protected static boolean preparePickedStack(BlockPos pos, BlockState state, BlockEntity be,
                                                World world, MinecraftClient mc)
    {
        ItemStack stack = state.getBlock().getPickStack(world, pos, state);

        if (stack.isEmpty() == false)
        {
            addBlockEntityNbt(stack, be);
            mc.player.getInventory().offHand.set(0, stack);
            mc.interactionManager.clickCreativeStack(stack, 45);
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
}
