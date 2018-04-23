package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.Selection;
import fi.dy.masa.litematica.util.Constants;
import fi.dy.masa.litematica.util.NBTUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LitematicaSchematic
{
    public static final int SCHEMATIC_VERSION = 1;
    private final Map<String, LitematicaBlockStateContainer> blockContainers = new HashMap<>();
    private final Map<String, Map<BlockPos, NBTTagCompound>> tileEntities = new HashMap<>();
    private final Map<String, List<EntityInfo>> entities = new HashMap<>();
    private final Map<String, BlockPos> subRegionPositions = new HashMap<>();
    private final Map<String, BlockPos> subRegionSizes = new HashMap<>();
    private final SchematicMetadata metadata = new SchematicMetadata();
    private BlockPos totalSize = BlockPos.ORIGIN;

    public BlockPos getTotalSize()
    {
        return this.totalSize;
    }

    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    @Nullable
    public static LitematicaSchematic makeSchematic(World world, Selection area, boolean takeEntities, String author, IStringConsumer feedback)
    {
        List<Box> boxes = getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.setString(I18n.format("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic();

        long time = (new Date()).getTime();
        schematic.totalSize = PositionUtils.getTotalAreaSize(area);
        schematic.metadata.setAuthor(author);
        schematic.metadata.setTimeCreated(time);
        schematic.metadata.setTimeModified(time);

        schematic.setSubRegionPositions(boxes, area.getOrigin());
        schematic.setSubRegionSizes(boxes);

        schematic.takeBlocksFromWorld(world, boxes, area.getOrigin());

        if (takeEntities)
        {
            schematic.takeEntitiesFromWorld(world, boxes, area.getOrigin());
        }

        return schematic;
    }

    private void takeEntitiesFromWorld(World world, List<Box> boxes, BlockPos origin)
    {
        for (Box box : boxes)
        {
            List<EntityInfo> list = new ArrayList<>();
            AxisAlignedBB bb = PositionUtils.createEnclosingAABB(box.getPos1(), box.getPos2());
            List<Entity> entities = world.getEntitiesInAABBexcluding(null, bb, null);

            for (Entity entity : entities)
            {
                NBTTagCompound tag = new NBTTagCompound();

                if (entity.writeToNBTOptional(tag))
                {
                    Vec3d posVec = new Vec3d(entity.posX - origin.getX(), entity.posY - origin.getY(), entity.posZ - origin.getZ());
                    BlockPos pos;

                    if (entity instanceof EntityPainting)
                    {
                        pos = ((EntityPainting) entity).getHangingPosition().subtract(origin);
                    }
                    else
                    {
                        pos = new BlockPos(posVec);
                    }

                    list.add(new EntityInfo(pos, posVec, tag));
                }
            }

            this.entities.put(box.getName(), list);
        }
    }

    private void takeBlocksFromWorld(World world, List<Box> boxes, BlockPos origin)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        for (Box box : boxes)
        {
            Map<BlockPos, NBTTagCompound> map = new HashMap<>();
            BlockPos size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());
            LitematicaBlockStateContainer container = new LitematicaBlockStateContainer(sizeX, sizeY, sizeZ);

            // We want to loop nice & easy from 0 to n here, but the per-sub-region pos1 can be at
            // any corner of the area. Thus we need to offset from the total area origin
            // to the minimum/negative corner (ie. 0,0 in the loop) corner here.
            BlockPos minCorner = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
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
                        container.set(x, y, z, state);

                        if (state.getBlock().hasTileEntity())
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                // TODO Add a TileEntity NBT cache from the Chunk packets, to get the original synced data (too)
                                BlockPos pos = new BlockPos(x + startX - origin.getX(), y + startY - origin.getY(), z + startZ - origin.getZ());
                                NBTTagCompound nbt = new NBTTagCompound();
                                nbt = te.writeToNBT(nbt);
                                map.put(pos, nbt);
                            }
                        }
                    }
                }
            }

            this.tileEntities.put(box.getName(), map);
            this.blockContainers.put(box.getName(), container);
        }
    }

    private void setSubRegionPositions(List<Box> boxes, BlockPos areaOrigin)
    {
        for (Box box : boxes)
        {
            this.subRegionPositions.put(box.getName(), box.getPos1().subtract(areaOrigin));
        }
    }

    private void setSubRegionSizes(List<Box> boxes)
    {
        for (Box box : boxes)
        {
            this.subRegionSizes.put(box.getName(), box.getSize());
        }
    }

    private static List<Box> getValidBoxes(Selection area)
    {
        List<Box> boxes = new ArrayList<>();
        Collection<Box> originalBoxes = area.getAllSelectionsBoxes();

        for (Box box : originalBoxes)
        {
            if (isBoxValid(box))
            {
                boxes.add(box);
            }
        }

        return boxes;
    }

    private static boolean isBoxValid(Box box)
    {
        return box.getPos1() != null && box.getPos2() != null;
    }

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setInteger("Version", SCHEMATIC_VERSION);
        nbt.setTag("TotalSize", NBTUtils.createBlockPosTag(this.totalSize));
        nbt.setTag("Metadata", this.metadata.writeToNBT());
        nbt.setTag("Regions", this.writeSubRegionsToNBT());

        return nbt;
    }

    private NBTTagCompound writeSubRegionsToNBT()
    {
        NBTTagCompound wrapper = new NBTTagCompound();

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainer blockContainer = this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.tileEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();

                tag.setTag("BlockStatePalette", blockContainer.getPalette().writeToNBT());
                tag.setIntArray("BlockStates", this.longArrayToIntArray(blockContainer.getBackingLongArray()));
                tag.setTag("TileEntities", this.writeTileEntitiesToNBT(tileMap));

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    tag.setTag("Entities", this.writeEntitiesToNBT(entityList));
                }

                BlockPos pos = this.subRegionPositions.get(regionName);
                tag.setTag("Position", NBTUtils.createBlockPosTag(pos));

                pos = this.subRegionSizes.get(regionName);
                tag.setTag("Size", NBTUtils.createBlockPosTag(pos));

                wrapper.setTag(regionName, tag);
            }
        }

        return wrapper;
    }

    private NBTTagList writeEntitiesToNBT(List<EntityInfo> entityList)
    {
        NBTTagList tagList = new NBTTagList();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                NBTTagCompound tag = new NBTTagCompound();

                NBTUtils.writeBlockPosToTag(info.pos, tag);
                NBTUtils.writeVec3dToTag(info.posVec, tag);
                tag.setTag("EntityData", info.nbt);

                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    private NBTTagList writeTileEntitiesToNBT(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (Map.Entry<BlockPos, NBTTagCompound> entry : tileMap.entrySet())
            {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound tileNbt = entry.getValue();
                tileNbt.removeTag("x");
                tileNbt.removeTag("y");
                tileNbt.removeTag("z");
                tag.setTag("TileNBT", entry.getValue());

                // Note: This within-schematic relative position is not inside the tile tag!
                NBTUtils.writeBlockPosToTag(entry.getKey(), tag);

                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    private boolean readFromNBT(NBTTagCompound nbt)
    {
        if (nbt.getInteger("Version") == SCHEMATIC_VERSION)
        {
            this.blockContainers.clear();
            this.tileEntities.clear();
            this.entities.clear();
            this.subRegionPositions.clear();
            this.subRegionSizes.clear();

            BlockPos size = NBTUtils.readBlockPos(nbt.getCompoundTag("TotalSize"));

            if (size != null)
            {
                this.totalSize = size;
            }

            this.metadata.readFromNBT(nbt.getCompoundTag("Metadata"));
            this.readSubRegionsFromNBT(nbt.getCompoundTag("Regions"));

            return true;
        }

        return false;
    }

    private void readSubRegionsFromNBT(NBTTagCompound tag)
    {
        for (String regionName : tag.getKeySet())
        {
            if (tag.getTag(regionName).getId() == Constants.NBT.TAG_COMPOUND)
            {
                NBTTagCompound regionTag = tag.getCompoundTag(regionName);
                BlockPos regionPos = NBTUtils.readBlockPos(regionTag.getCompoundTag("Position"));
                BlockPos regionSize = NBTUtils.readBlockPos(regionTag.getCompoundTag("Size"));

                if (regionPos != null && regionSize != null)
                {
                    this.subRegionPositions.put(regionName, regionPos);
                    this.subRegionSizes.put(regionName, regionSize);

                    this.tileEntities.put(regionName, this.readTileEntitiesFromNBT(regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND)));
                    this.entities.put(regionName, this.readEntitiesFromNBT(regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND)));

                    NBTTagList palette = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                    long[] blockStateArr = this.intArrayToLongArray(regionTag.getIntArray("BlockStates"));
                    LitematicaBlockStateContainer container = LitematicaBlockStateContainer.createFrom(palette, blockStateArr, regionSize);
                    this.blockContainers.put(regionName, container);
                }
            }
        }
    }

    private List<EntityInfo> readEntitiesFromNBT(NBTTagList tagList)
    {
        List<EntityInfo> entityList = new ArrayList<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);
            Vec3d posVec = NBTUtils.readVec3d(tag);
            NBTTagCompound entityData = tag.getCompoundTag("EntityData");

            if (pos != null && posVec != null && entityData.hasNoTags() == false)
            {
                entityList.add(new EntityInfo(pos, posVec, entityData));
            }
        }

        return entityList;
    }

    private Map<BlockPos, NBTTagCompound> readTileEntitiesFromNBT(NBTTagList tagList)
    {
        Map<BlockPos, NBTTagCompound> tileMap = new HashMap<>();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagCompound tileNbt = tag.getCompoundTag("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tileNbt.hasNoTags() == false)
            {
                tileMap.put(pos, tileNbt);
            }
        }

        return tileMap;
    }

    private int[] longArrayToIntArray(long[] arrLong)
    {
        int[] arrInt = new int[arrLong.length * 2];

        for (int i = 0; i < arrLong.length; ++i)
        {
            arrInt[i * 2    ] = (int) ( arrLong[i]         & 0xFFFFFFFF);
            arrInt[i * 2 + 1] = (int) ((arrLong[i] >>> 32) & 0xFFFFFFFF);
        }

        return arrInt;
    }

    private long[] intArrayToLongArray(int[] arrInt)
    {
        long[] arrLong = new long[(int) Math.ceil(arrInt.length / 2)];
        final int maxLower = (int) Math.floor(arrInt.length / 2);

        for (int i = 0; i < maxLower; ++i)
        {
            int intIndex = i << 1;
            arrLong[i] = (((long) arrInt[intIndex + 1]) << 32) | arrInt[intIndex];
        }

        // Odd number of input elements, handle the last element
        if (maxLower != arrLong.length)
        {
            arrLong[arrLong.length - 1] = arrInt[arrInt.length - 1];
        }

        return arrLong;
    }

    public boolean writeToFile(File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(".litematic") == false)
        {
            fileName = fileName + ".litematic";
        }

        File fileSchematic = new File(dir, fileName);
        File fileMeta = new File(dir, fileName + "_meta");

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && fileSchematic.exists())
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exists", fileSchematic.getAbsolutePath()));
                return false;
            }

            if (override == false && fileMeta.exists())
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exists", fileMeta.getAbsolutePath()));
                return false;
            }

            FileOutputStream os = new FileOutputStream(fileSchematic);
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();

            os = new FileOutputStream(fileMeta);
            CompressedStreamTools.writeCompressed(this.metadata.writeToNBT(), os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileNameIn, IStringConsumer feedback)
    {
        String fileName = fileNameIn;

        if (fileName.endsWith(".litematic") == false)
        {
            fileName = fileName + ".litematic";
        }

        File fileSchematic = new File(dir, fileName);

        if (fileSchematic.exists() == false || fileSchematic.canRead() == false)
        {
            feedback.setString(I18n.format("litematica.error.schematic_read_from_file_failed.cant_read", fileSchematic.getAbsolutePath()));
            return null;
        }

        try
        {
            FileInputStream is = new FileInputStream(fileSchematic);
            NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
            is.close();

            if (nbt != null)
            {
                LitematicaSchematic schematic = new LitematicaSchematic();

                if (schematic.readFromNBT(nbt))
                {
                    feedback.setString(I18n.format("litematica.message.schematic_read_from_file_success", fileSchematic.getAbsolutePath()));
                    return schematic;
                }
            }
        }
        catch (Exception e)
        {
            feedback.setString(I18n.format("litematica.error.schematic_read_from_file_failed.exception", fileSchematic.getAbsolutePath()));
        }

        return null;
    }

    public static class EntityInfo
    {
        public final BlockPos pos;
        public final Vec3d posVec;
        public final NBTTagCompound nbt;

        public EntityInfo(BlockPos pos, Vec3d posVec, NBTTagCompound nbt)
        {
            this.pos = pos;
            this.posVec = posVec;
            this.nbt = nbt;
        }
    }
}
