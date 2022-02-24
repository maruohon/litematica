package fi.dy.masa.litematica.schematic.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.EntityInfo;
import fi.dy.masa.litematica.schematic.ISchematic;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicMetadata;
import fi.dy.masa.litematica.schematic.SchematicType;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.overlay.message.MessageDispatcher;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.ResultingStringConsumer;
import fi.dy.masa.malilib.util.nbt.NbtUtils;
import fi.dy.masa.malilib.util.position.IntBoundingBox;

public class SchematicCreationUtils
{
    @Nullable
    public static <S extends ISchematic> S createFromFile(File file, Function<File, S> factory)
    {
        S schematic = factory.apply(file);

        if (schematic.readFromFile())
        {
            return schematic;
        }

        return null;
    }

    @Nullable
    public static <S extends ISchematic> S createFromSchematic(ISchematic other, Supplier<S> factory)
    {
        S schematic = factory.get();
        schematic.readFrom(other);
        return schematic;
    }

    /**
     * Creates an empty schematic with all the maps and lists and containers already created.
     * This is intended to be used for the chunk-wise schematic creation.
     */
    public static LitematicaSchematic createEmptySchematic(AreaSelection area, String author)
    {
        List<SelectionBox> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            MessageDispatcher.error().translate("litematica.error.schematic.create.no_selections");
            return null;
        }

        LitematicaSchematic schematic = SchematicType.LITEMATICA.createSchematic(null);
        SchematicMetadata metadata = schematic.getMetadata();

        schematic.setSubRegions(boxes, area.getEffectiveOrigin());

        metadata.setAuthor(author);
        metadata.setName(area.getName());
        metadata.setRegionCount(boxes.size());
        metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));

        return schematic;
    }

    @Nullable
    public static LitematicaSchematic createFromWorld(World world, AreaSelection area, boolean ignoreEntities, String author, ResultingStringConsumer feedback)
    {
        List<SelectionBox> boxes = PositionUtils.getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.consumeString(StringUtils.translate("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = SchematicType.LITEMATICA.createSchematic(null);
        long time = System.currentTimeMillis();

        BlockPos origin = area.getEffectiveOrigin();
        schematic.setSubRegions(boxes, origin);

        takeBlocksFromWorld(schematic, world, boxes);

        if (ignoreEntities == false)
        {
            takeEntitiesFromWorld(schematic, world, boxes, origin);
        }

        SchematicMetadata metadata = schematic.getMetadata();
        metadata.setAuthor(author);
        metadata.setName(area.getName());
        metadata.setTimeCreated(time);
        metadata.setTimeModified(time);
        metadata.setRegionCount(boxes.size());
        metadata.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        metadata.setEnclosingSize(PositionUtils.getEnclosingAreaSize(boxes));
        metadata.setTotalBlocks(schematic.getTotalBlocksReadFromWorld());

        return schematic;
    }

    private static void takeEntitiesFromWorld(LitematicaSchematic schematic, World world, List<SelectionBox> boxes, BlockPos origin)
    {
        for (SelectionBox box : boxes)
        {
            String regionName = box.getName();
            ISchematicRegion region = schematic.getSchematicRegion(regionName);
            List<EntityInfo> schematicEntityList = region != null ? region.getEntityList() : null;

            if (schematicEntityList == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_save.missing_entity_list", box.getName());
                continue;
            }

            AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            BlockPos regionPosAbs = box.getPos1();
            List<EntityInfo> list = new ArrayList<>();
            List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, null);

            for (Entity entity : entities)
            {
                NBTTagCompound tag = new NBTTagCompound();

                if (entity.writeToNBTOptional(tag))
                {
                    Vec3d posVec = new Vec3d(entity.posX - regionPosAbs.getX(), entity.posY - regionPosAbs.getY(), entity.posZ - regionPosAbs.getZ());
                    NbtUtils.writeVec3dToListTag(posVec, tag);
                    list.add(new EntityInfo(posVec, tag));
                }
            }

            schematicEntityList.addAll(list);
        }
    }

    public static void takeEntitiesFromWorldWithinChunk(LitematicaSchematic schematic, World world, int chunkX, int chunkZ,
            ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, SelectionBox> boxes, Set<UUID> existingEntities, BlockPos origin)
    {
        for (Map.Entry<String, IntBoundingBox> entry : volumes.entrySet())
        {
            String regionName = entry.getKey();
            Box box = boxes.get(regionName);
            ISchematicRegion region = schematic.getSchematicRegion(regionName);
            List<EntityInfo> schematicEntityList = region != null ? region.getEntityList() : null;

            if (box == null || schematicEntityList == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_save.missing_entity_list", regionName);
                continue;
            }

            AxisAlignedBB bb = PositionUtils.createAABBFrom(entry.getValue());
            List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, null);
            BlockPos regionPosAbs = box.getPos1();

            for (Entity entity : entities)
            {
                UUID uuid = entity.getUniqueID();
                /*
                if (entity.posX >= bb.minX && entity.posX < bb.maxX &&
                    entity.posY >= bb.minY && entity.posY < bb.maxY &&
                    entity.posZ >= bb.minZ && entity.posZ < bb.maxZ)
                */
                if (existingEntities.contains(uuid) == false)
                {
                    NBTTagCompound tag = new NBTTagCompound();

                    if (entity.writeToNBTOptional(tag))
                    {
                        Vec3d posVec = new Vec3d(entity.posX - regionPosAbs.getX(), entity.posY - regionPosAbs.getY(), entity.posZ - regionPosAbs.getZ());
                        NbtUtils.writeVec3dToListTag(posVec, tag);
                        schematicEntityList.add(new EntityInfo(posVec, tag));
                        existingEntities.add(uuid);
                    }
                }
            }
        }
    }

    private static void takeBlocksFromWorld(LitematicaSchematic schematic, World world, List<SelectionBox> boxes)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        long totalBlocks = 0;

        for (SelectionBox box : boxes)
        {
            String regionName = box.getName();
            ISchematicRegion region = schematic.getSchematicRegion(regionName);
            ILitematicaBlockStateContainer container = region != null ? region.getBlockStateContainer() : null;
            Map<BlockPos, NBTTagCompound> blockEntityMap = region != null ? region.getBlockEntityMap() : null;
            Map<BlockPos, NextTickListEntry> tickMap = region != null ? region.getBlockTickMap() : null;

            if (container == null || blockEntityMap == null || tickMap == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_save.missing_container", regionName);
                continue;
            }

            Vec3i size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int startX = minCorner.getX();
            final int startY = minCorner.getY();
            final int startZ = minCorner.getZ();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        posMutable.setPos(x + startX, y + startY, z + startZ);
                        IBlockState state = world.getBlockState(posMutable).getActualState(world, posMutable);
                        container.setBlockState(x, y, z, state);

                        if (state.getBlock() != Blocks.AIR)
                        {
                            ++totalBlocks;
                        }

                        if (state.getBlock().hasTileEntity())
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x, y, z);
                                NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
                                NbtUtils.writeBlockPosToTag(pos, tag);
                                blockEntityMap.put(pos, tag);
                            }
                        }
                    }
                }
            }

            if (world instanceof WorldServer)
            {
                IntBoundingBox structureBB = IntBoundingBox.createProper(
                        startX,         startY,         startZ,
                        startX + sizeX, startY + sizeY, startZ + sizeZ);
                List<NextTickListEntry> pendingTicks = world.getPendingBlockUpdates(structureBB.toVanillaBox(), false);

                if (pendingTicks != null)
                {
                    final int listSize = pendingTicks.size();
                    final long currentTime = world.getTotalWorldTime();

                    // The getPendingBlockUpdates() method doesn't check the y-coordinate... :-<
                    for (int i = 0; i < listSize; ++i)
                    {
                        NextTickListEntry entry = pendingTicks.get(i);

                        if (entry.position.getY() >= startY && entry.position.getY() < structureBB.maxY)
                        {
                            // Store the delay, ie. relative time
                            BlockPos posRelative = new BlockPos(
                                    entry.position.getX() - minCorner.getX(),
                                    entry.position.getY() - minCorner.getY(),
                                    entry.position.getZ() - minCorner.getZ());
                            NextTickListEntry newEntry = new NextTickListEntry(posRelative, entry.getBlock());
                            newEntry.setPriority(entry.priority);
                            newEntry.setScheduledTime(entry.scheduledTime - currentTime);

                            tickMap.put(posRelative, newEntry);
                        }
                    }
                }
            }
        }

        schematic.setTotalBlocksReadFromWorld(totalBlocks);
    }

    public static void takeBlocksFromWorldWithinChunk(LitematicaSchematic schematic, World world, int chunkX, int chunkZ,
            ImmutableMap<String, IntBoundingBox> volumes, ImmutableMap<String, SelectionBox> boxes)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
        long totalBlocks = schematic.getTotalBlocksReadFromWorld();

        for (Map.Entry<String, IntBoundingBox> volumeEntry : volumes.entrySet())
        {
            String regionName = volumeEntry.getKey();
            ISchematicRegion region = schematic.getSchematicRegion(regionName);
            IntBoundingBox bb = volumeEntry.getValue();
            Box box = boxes.get(regionName);

            if (box == null || region == null)
            {
                Litematica.logger.error("null Box for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            ILitematicaBlockStateContainer container = region.getBlockStateContainer();
            Map<BlockPos, NBTTagCompound> blockEntityMap = region.getBlockEntityMap();
            Map<BlockPos, NextTickListEntry> tickMap = region.getBlockTickMap();

            if (container == null || blockEntityMap == null || tickMap == null)
            {
                MessageDispatcher.error().translate("litematica.message.error.schematic_save.missing_container", regionName);
                Litematica.logger.error("null map(s) for sub-region '{}' while trying to save chunk-wise schematic", regionName);
                continue;
            }

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            final BlockPos minCorner = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            final int offsetX = minCorner.getX();
            final int offsetY = minCorner.getY();
            final int offsetZ = minCorner.getZ();
            // Relative coordinates within the sub-region container:
            final int startX = bb.minX - minCorner.getX();
            final int startY = bb.minY - minCorner.getY();
            final int startZ = bb.minZ - minCorner.getZ();
            final int endX = startX + (bb.maxX - bb.minX);
            final int endY = startY + (bb.maxY - bb.minY);
            final int endZ = startZ + (bb.maxZ - bb.minZ);

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.setPos(x + offsetX, y + offsetY, z + offsetZ);
                        IBlockState state = world.getBlockState(posMutable).getActualState(world, posMutable);
                        container.setBlockState(x, y, z, state);

                        if (state.getBlock() != Blocks.AIR)
                        {
                            ++totalBlocks;
                        }

                        if (state.getBlock().hasTileEntity())
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x, y, z);
                                NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
                                NbtUtils.writeBlockPosToTag(pos, tag);
                                blockEntityMap.put(pos, tag);
                            }
                        }
                    }
                }
            }

            if (world instanceof WorldServer)
            {
                IntBoundingBox structureBB = IntBoundingBox.createProper(
                        offsetX + startX  , offsetY + startY  , offsetZ + startZ  ,
                        offsetX + endX + 1, offsetY + endY + 1, offsetZ + endZ + 1);
                List<NextTickListEntry> pendingTicks = ((WorldServer) world).getPendingBlockUpdates(structureBB.toVanillaBox(), false);

                if (pendingTicks != null)
                {
                    final int listSize = pendingTicks.size();
                    final long currentTime = world.getTotalWorldTime();

                    // The getPendingBlockUpdates() method doesn't check the y-coordinate... :-<
                    for (int i = 0; i < listSize; ++i)
                    {
                        NextTickListEntry entry = pendingTicks.get(i);

                        if (entry.position.getY() >= offsetY && entry.position.getY() < structureBB.maxY)
                        {
                            // Store the delay, ie. relative time
                            BlockPos posRelative = new BlockPos(
                                    entry.position.getX() - minCorner.getX(),
                                    entry.position.getY() - minCorner.getY(),
                                    entry.position.getZ() - minCorner.getZ());
                            NextTickListEntry newEntry = new NextTickListEntry(posRelative, entry.getBlock());
                            newEntry.setPriority(entry.priority);
                            newEntry.setScheduledTime(entry.scheduledTime - currentTime);

                            tickMap.put(posRelative, newEntry);
                        }
                    }
                }
            }
        }

        schematic.setTotalBlocksReadFromWorld(totalBlocks);
    }
}
