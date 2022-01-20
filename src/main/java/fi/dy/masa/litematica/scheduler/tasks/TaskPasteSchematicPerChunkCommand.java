package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
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
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class TaskPasteSchematicPerChunkCommand extends TaskPasteSchematicPerChunkBase
{
    protected final LongArrayList fillVolumes = new LongArrayList();
    protected final BlockPos.Mutable mutablePos = new BlockPos.Mutable();
    protected final PasteNbtBehavior nbtBehavior;
    protected final String fillCommand;
    protected final String setBlockCommand;
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
        this.fillCommand = Configs.Generic.COMMAND_NAME_FILL.getStringValue();
        this.setBlockCommand = Configs.Generic.COMMAND_NAME_SETBLOCK.getStringValue();
        this.useFillCommand = Configs.Generic.PASTE_USE_FILL_COMMAND.getBooleanValue();
        this.useWorldEdit = Configs.Generic.COMMAND_USE_WORLDEDIT.getBooleanValue();
        this.nbtBehavior = (PasteNbtBehavior) Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue();

        if (this.useFillCommand)
        {
            this.processBoxBlocksTask = this::processBlocksInCurrentBoxUsingFill;
        }
        else
        {
            this.processBoxBlocksTask = this::processBlocksInCurrentBoxUsingSetblockOnly;
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

        if (this.useWorldEdit)
        {
            this.mc.player.sendChatMessage("//perf neighbors off");
        }

        this.phase = TaskPhase.PROCESS_BOX_BLOCKS;
    }

    protected void prepareSummoningEntities(IntBoundingBox box)
    {
        net.minecraft.util.math.Box bb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        this.entityIterator = this.schematicWorld.getOtherEntities(null, bb, (e) -> true).iterator();
        this.phase = TaskPhase.PROCESS_BOX_ENTITIES;
    }

    protected void processBlocksInCurrentBoxUsingSetblockOnly()
    {
        ChunkPos chunkPos = this.currentChunkPos;
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

        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               this.fillVolumes.isEmpty() == false)
        {
            int index = this.fillVolumes.size() - 1;
            long encodedValue = this.fillVolumes.removeLong(index);
            this.fillVolume(encodedValue, baseX, baseZ, schematicChunk, clientChunk);
        }

        if (this.fillVolumes.isEmpty())
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
        while (this.sentCommandsThisTick < this.maxCommandsPerTick &&
               this.entityIterator.hasNext())
        {
            this.summonEntity(this.entityIterator.next());
        }

        if (this.entityIterator.hasNext() == false)
        {
            this.onFinishedProcessingBox(this.currentChunkPos, this.currentBox);
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
        String blockString = BlockArgumentParser.stringifyBlockState(state);

        if (this.useWorldEdit)
        {
            this.sendCommand(String.format("//pos1 %d,%d,%d", x, y, z), player);
            this.sendCommand(String.format("//pos2 %d,%d,%d", x, y, z), player);
            this.sendCommand("//set " + blockString, player);
        }
        else
        {
            String cmdName = this.setBlockCommand;
            String strCommand = String.format("%s %d %d %d %s", cmdName, x, y, z, blockString);

            this.sendCommand(strCommand, player);
        }

        ++this.sentSetblockCommands;
    }

    protected void sendFillCommand(int x, int y, int z, int x2, int y2, int z2,
                                   BlockState state, ClientPlayerEntity player)
    {
        String blockString = BlockArgumentParser.stringifyBlockState(state);

        if (this.useWorldEdit)
        {
            this.sendCommand(String.format("//pos1 %d,%d,%d", x, y, z), player);
            this.sendCommand(String.format("//pos2 %d,%d,%d", x2, y2, z2), player);
            this.sendCommand("//set " + blockString, player);
        }
        else
        {
            String cmdName = this.fillCommand;
            String strCommand = String.format("%s %d %d %d %d %d %d %s", cmdName, x, y, z, x2, y2, z2, blockString);

            if (this.replace == ReplaceBehavior.NONE ||
                (this.replace == ReplaceBehavior.WITH_NON_AIR && state.isAir()))
            {
                strCommand += " replace air";
            }

            this.sendCommand(strCommand, player);
        }

        ++this.sentFillCommands;
    }

    protected void setDataViaDataModify(BlockPos pos, BlockState state, BlockEntity be,
                                        World schematicWorld, ClientWorld clientWorld, ClientPlayerEntity player)
    {
        BlockPos placementPos = findEmptyNearbyPosition(clientWorld, player.getBlockPos(), 3);

        if (placementPos != null && preparePickedStack(pos, state, be, schematicWorld, this.mc))
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

        if (placementPos != null && preparePickedStack(pos, state, be, schematicWorld, this.mc))
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
        if (preparePickedStack(pos, state, be, schematicWorld, this.mc))
        {
            player.setPos(pos.getX(), pos.getY() + 2, pos.getZ());

            String command = String.format("tp @p %d %d %d", pos.getX(), pos.getY() + 2, pos.getZ());
            this.sendCommand(command, player);

            Vec3d posVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(posVec, Direction.UP, pos, false);
            this.mc.interactionManager.interactBlock(player, clientWorld, Hand.OFF_HAND, hitResult);
        }
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

        if (endOffsetX > 0 || endOffsetY > 0 || endOffsetZ > 0)
        {
            int endX = startX + endOffsetX;
            int endY = startY + endOffsetY;
            int endZ = startZ + endOffsetZ;
            BlockState state = schematicChunk.getBlockState(this.mutablePos);

            //System.out.printf("fill @ [%d %d %d] -> [%d %d %d] (%d x %d x %d) %s\n", startX, startY, startZ, endX, endY, endZ, endOffsetX + 1, endOffsetY + 1, endOffsetZ + 1, state);
            this.sendFillCommand(startX, startY, startZ, endX, endY, endZ, state, this.mc.player);
        }
        else
        {
            //System.out.printf("fill -> setblock @ [%d %d %d] %s\n", startX, startY, startZ, schematicChunk.getBlockState(this.mutablePos));
            this.pasteBlock(this.mutablePos, schematicChunk, clientChunk);
        }
    }

    protected void generateFillVolumes(IntBoundingBox box)
    {
        ChunkSchematic chunk = this.schematicWorld.getChunkProvider().getChunk(box.minX >> 4, box.minZ >> 4);
        this.fillVolumes.clear();

        if (this.workArr == null)
        {
            int height = this.world.getHeight();
            this.workArr = new int[16][height][16];
        }

        this.generateStrips(this.workArr, Direction.EAST, box, chunk);
        this.combineStripsToLayers(this.workArr, Direction.EAST, Direction.SOUTH, Direction.UP, box, chunk, this.fillVolumes);
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
                                  ChunkSchematic chunk)
    {
        BlockPos.Mutable mutablePos = this.mutablePos;
        ReplaceBehavior replace = this.replace;
        final int startX = box.minX & 0xF;
        final int startZ = box.minZ & 0xF;
        final int endX = box.maxX & 0xF;
        final int endZ = box.maxZ & 0xF;

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
                        int length = this.getBlockStripLength(mutablePos, stripDirection, endX - x + 1, state, chunk);
                        workArr[x][y][z] = length;
                        //System.out.printf("strip @ [%d %d %d] %d x %s\n", x, y, z, length, state);
                        x += length - 1;
                    }
                }
            }
        }
    }

    protected void combineStripsToLayers(int[][][] workArr,
                                         Direction stripDirection,
                                         Direction stripCombineDirection,
                                         Direction layerCombineDirection,
                                         IntBoundingBox box,
                                         ChunkSchematic chunk,
                                         LongArrayList volumesOut)
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

        for (int y = box.minY; y <= box.maxY; ++y)
        {
            for (int x = startX; x <= endX; ++x)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    int length = workArr[x][y][z];

                    if (length > 0)
                    {
                        int nextX = x + scOffX;
                        int nextY = y + scOffY;
                        int nextZ = z + scOffZ;
                        int stripCount = 1;

                        mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(mutablePos);

                        // Find identical adjacent strips, and set their data in the array to zero,
                        // since they are being combined into one layer starting from the first position.
                        while (nextX <= 15 && nextY <= box.maxY && nextZ <= 15 &&
                               workArr[nextX][nextY][nextZ] == length &&
                               chunk.getBlockState(mutablePos.set(nextX, nextY, nextZ)) == state)
                        {
                            ++stripCount;
                            workArr[nextX][nextY][nextZ] = 0;
                            nextX += scOffX;
                            nextY += scOffY;
                            nextZ += scOffZ;
                        }

                        // Encode the first two dimensions of the volume (at this point a layer).
                        // Note: At this point the range is 1...16 so that it can be distinguished from "no data" = 0
                        int packedX = sdOffX * length + scOffX * stripCount;
                        int packedY = sdOffY * length + scOffY * stripCount;
                        int packedZ = sdOffZ * length + scOffZ * stripCount;
                        int packedSize = packCoordinate5bit(packedX, packedY, packedZ);

                        //System.out.printf("layer @ [%d %d %d] len: %d x count: %d %s\n", x, y, z, length, stripCount, state);
                        workArr[x][y][z] = packedSize;

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
                    int packedSize = workArr[x][y][z];

                    if (packedSize > 0)
                    {
                        int nextX = x + lcOffX;
                        int nextY = y + lcOffY;
                        int nextZ = z + lcOffZ;
                        int layerCount = 1;

                        mutablePos.set(x, y, z);
                        BlockState state = chunk.getBlockState(mutablePos);

                        // Find identical adjacent layers
                        while (nextX <= 15 && nextY <= box.maxY && nextZ <= 15 &&
                               workArr[nextX][nextY][nextZ] == packedSize &&
                               chunk.getBlockState(mutablePos.set(nextX, nextY, nextZ)) == state)
                        {
                            ++layerCount;
                            workArr[nextX][nextY][nextZ] = 0;
                            nextX += lcOffX;
                            nextY += lcOffY;
                            nextZ += lcOffZ;
                        }

                        // Add the layer thickness, and change the encoding from 1...16 to 0...15
                        // All the axes here will have values of at least 1 before this -1.
                        int volumeEndOffsetX = lcOffX * layerCount + unpackX5bit(packedSize) - 1;
                        int volumeEndOffsetY = lcOffY * layerCount + unpackY5bit(packedSize) - 1;
                        int volumeEndOffsetZ = lcOffZ * layerCount + unpackZ5bit(packedSize) - 1;
                        int packedVolumeEndOffset = packCoordinate(volumeEndOffsetX, volumeEndOffsetY, volumeEndOffsetZ);

                        //System.out.printf("volume @ [%d %d %d] size: %d x %d x %d %s\n", x, y, z, volumeEndOffsetX, volumeEndOffsetY, volumeEndOffsetZ, state);
                        long encodedValue = ((long) packedVolumeEndOffset << 32L) | packCoordinate(x, y, z);
                        volumesOut.add(encodedValue);

                        // Always also clear the array for the next use
                        workArr[x][y][z] = 0;

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

    public static boolean preparePickedStack(BlockPos pos,
                                             BlockState state,
                                             BlockEntity be,
                                             World world,
                                             MinecraftClient mc)
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
