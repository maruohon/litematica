package fi.dy.masa.litematica.scheduler.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PasteNbtBehavior;
import fi.dy.masa.litematica.util.ReplaceBehavior;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.PositionUtils;

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
    private int sentSetblockCommands;
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
        ClientWorld worldClient = this.mc.world;
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

    @Override
    protected void onChunkListSorted()
    {
        super.onChunkListSorted();

        this.boxesInCurrentChunk.clear();
        this.boxesInCurrentChunk.addAll(this.boxesInChunks.get(this.chunks.get(0)));
    }

    protected boolean processBox(ChunkPos pos, IntBoundingBox box,
                                 WorldSchematic worldSchematic,
                                 ClientWorld worldClient,
                                 ClientPlayerEntity player)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable();
        Chunk chunkSchematic = worldSchematic.getChunkProvider().getChunk(pos.x, pos.z);
        Chunk chunkClient = worldClient.getChunk(pos.x, pos.z);
        PasteNbtBehavior nbtBehavior = (PasteNbtBehavior) Configs.Generic.PASTE_NBT_BEHAVIOR.getOptionListValue();

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

            BlockState stateSchematic = chunkSchematic.getBlockState(posMutable);
            BlockState stateClient = chunkClient.getBlockState(posMutable);

            if (stateSchematic.isAir() == false || stateClient.isAir() == false)
            {
                if (this.changedBlockOnly == false || stateClient != stateSchematic)
                {
                    if ((this.replace == ReplaceBehavior.NONE && stateClient.isAir() == false) ||
                                (this.replace == ReplaceBehavior.WITH_NON_AIR && stateSchematic.isAir()))
                    {
                        continue;
                    }

                    BlockEntity be = worldSchematic.getBlockEntity(posMutable);

                    if (be != null && nbtBehavior != PasteNbtBehavior.NONE)
                    {
                        if (nbtBehavior == PasteNbtBehavior.PLACE_MODIFY)
                        {
                            this.setDataViaDataModify(posMutable, stateSchematic, be, worldSchematic, worldClient, player);
                        }
                        else if (nbtBehavior == PasteNbtBehavior.PLACE_CLONE)
                        {
                            this.placeBlockViaClone(posMutable, stateSchematic, be, worldSchematic, worldClient, player);
                        }
                        else if (nbtBehavior == PasteNbtBehavior.TELEPORT_PLACE)
                        {
                            this.placeBlockDirectly(posMutable, stateSchematic, be, worldSchematic, worldClient, player);
                        }
                    }
                    else
                    {
                        this.sendSetBlockCommand(posMutable.getX(), posMutable.getY(), posMutable.getZ(), stateSchematic, player);
                    }

                    if (this.sentCommandsThisTick >= this.maxCommandsPerTick)
                    {
                        break;
                    }
                }
            }
        }

        if (this.currentIndex >= this.boxVolume)
        {
            if (Configs.Generic.PASTE_IGNORE_ENTITIES.getBooleanValue() == false)
            {
                this.summonEntities(box, worldSchematic, player);
            }

            this.boxInProgress = false;

            return true;
        }

        return false;
    }

    protected void summonEntities(IntBoundingBox box, WorldSchematic worldSchematic, ClientPlayerEntity player)
    {
        net.minecraft.util.math.Box bb = new net.minecraft.util.math.Box(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1);
        List<Entity> entities = worldSchematic.getOtherEntities(null, bb, (e) -> true);

        for (Entity entity : entities)
        {
            String id = EntityUtils.getEntityId(entity);

            if (id != null)
            {
                /*
                NBTTagCompound nbt = new NBTTagCompound();
                entity.writeToNBTOptional(nbt);
                String nbtString = nbt.toString();
                */

                String strCommand = String.format(Locale.ROOT, "summon %s %f %f %f", id, entity.getX(), entity.getY(), entity.getZ());
                /*
                String strCommand = String.format("/summon %s %f %f %f %s", entityName, entity.posX, entity.posY, entity.posZ, nbtString);
                System.out.printf("entity: %s\n", entity);
                System.out.printf("%s\n", strCommand);
                System.out.printf("nbt: %s\n", nbtString);
                */

                this.sendCommand(strCommand, player);
            }
        }
    }

    protected void sendSetBlockCommand(int x, int y, int z, BlockState state, ClientPlayerEntity player)
    {
        String cmdName = Configs.Generic.PASTE_COMMAND_SETBLOCK.getStringValue();
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

            Set<String> keys = new HashSet<>();

            try
            {
                keys.addAll(be.createNbt().getKeys());
            } catch (Exception ignore) {}

            keys.remove("id");
            keys.remove("x");
            keys.remove("y");
            keys.remove("z");

            this.sendSetBlockCommand(pos.getX(), pos.getY(), pos.getZ(), state, player);

            for (String key : keys)
            {
                String command = String.format("data modify block %d %d %d %s set from block %d %d %d %s",
                                               pos.getX(), pos.getY(), pos.getZ(), key,
                                               placementPos.getX(), placementPos.getY(), placementPos.getZ(), key);
                this.sendCommand(command, player);
            }

            String cmdName = Configs.Generic.PASTE_COMMAND_SETBLOCK.getStringValue();
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

            {
                String command = String.format("data get block %d %d %d", placementPos.getX(), placementPos.getY(), placementPos.getZ());
                this.sendCommand(command, player);
            }

            String command = String.format("clone %d %d %d %d %d %d %d %d %d",
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                                           pos.getX(), pos.getY(), pos.getZ());
            this.sendCommand(command, player);

            command = String.format("setblock %d %d %d air", placementPos.getX(), placementPos.getY(), placementPos.getZ());
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

        if (this.mc.player != null)
        {
            this.mc.player.sendChatMessage("/gamerule sendCommandFeedback true");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        super.stop();
    }
}
