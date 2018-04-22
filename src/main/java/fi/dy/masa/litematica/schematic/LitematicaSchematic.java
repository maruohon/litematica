package fi.dy.masa.litematica.schematic;

import java.io.File;
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
    private final List<LitematicaBlockStateContainer> blockContainers = new ArrayList<>();
    private Map<BlockPos, NBTTagCompound> tileEntities = new HashMap<>();
    private List<EntityInfo> entities = new ArrayList<>();
    private final List<BlockPos> subRegionPositions = new ArrayList<>();
    private final List<BlockPos> subRegionSizes = new ArrayList<>();
    private BlockPos totalSize = BlockPos.ORIGIN;
    private String author = "Unknown";
    private long timeCreated;
    private long timeModified;
    private String iconData = "";

    public BlockPos getTotalSize()
    {
        return this.totalSize;
    }

    public String getAuthor()
    {
        return this.author;
    }

    /**
     * @return the BASE64 encoded PNG image data, if set, or otherwise an empty string
     */
    public String getIconData()
    {
        return this.iconData;
    }

    public long getCreationTime()
    {
        return this.timeCreated;
    }

    public long getModificationTime()
    {
        return this.timeModified;
    }

    public boolean hasBeenModified()
    {
        return this.timeModified != this.timeCreated;
    }

    public void setIconData(String iconData)
    {
        this.iconData = iconData;
    }

    @Nullable
    public static LitematicaSchematic makeSchematic(World world, AreaSelection area, boolean takeEntities, String author, IStringConsumer feedback)
    {
        List<SelectionBox> boxes = getValidBoxes(area);

        if (boxes.isEmpty())
        {
            feedback.setString(I18n.format("litematica.error.schematic.create.no_selections"));
            return null;
        }

        LitematicaSchematic schematic = new LitematicaSchematic();

        schematic.totalSize = PositionUtils.getTotalAreaSize(area);
        schematic.author = author;
        schematic.timeCreated = (new Date()).getTime();
        schematic.timeModified = schematic.timeCreated;

        schematic.setSubRegionPositions(boxes, area.getOrigin());
        schematic.setSubRegionSizes(boxes);

        schematic.takeBlocksFromWorld(world, boxes, area.getOrigin());

        if (takeEntities)
        {
            schematic.takeEntitiesFromWorld(world, boxes, area.getOrigin());
        }

        return schematic;
    }

    private void takeEntitiesFromWorld(World world, List<SelectionBox> boxes, BlockPos origin)
    {
        for (SelectionBox box : boxes)
        {
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

                    this.entities.add(new EntityInfo(pos, posVec, tag));
                }
            }
        }
    }

    private void takeBlocksFromWorld(World world, List<SelectionBox> boxes, BlockPos origin)
    {
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);

        for (SelectionBox box : boxes)
        {
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
                                this.tileEntities.put(pos, nbt);
                            }
                        }
                    }
                }
            }

            this.blockContainers.add(container);
        }
    }

    private void setSubRegionPositions(List<SelectionBox> boxes, BlockPos areaOrigin)
    {
        for (SelectionBox box : boxes)
        {
            this.subRegionPositions.add(box.getPos1().subtract(areaOrigin));
        }
    }

    private void setSubRegionSizes(List<SelectionBox> boxes)
    {
        for (SelectionBox box : boxes)
        {
            this.subRegionSizes.add(box.getSize());
        }
    }

    private static List<SelectionBox> getValidBoxes(AreaSelection area)
    {
        List<SelectionBox> boxes = new ArrayList<>();
        Collection<SelectionBox> originalBoxes = area.getAllSelectionsBoxes();

        for (SelectionBox box : originalBoxes)
        {
            if (isBoxValid(box))
            {
                boxes.add(box);
            }
        }

        return boxes;
    }

    private static boolean isBoxValid(SelectionBox box)
    {
        return box.getPos1() != null && box.getPos2() != null;
    }

    private NBTTagCompound writeToNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setInteger("SizeX", this.totalSize.getX());
        nbt.setInteger("SizeY", this.totalSize.getY());
        nbt.setInteger("SizeZ", this.totalSize.getZ());
        nbt.setString("Author", this.author);
        nbt.setLong("TimeCreated", this.timeCreated);
        nbt.setLong("TimeModified", this.timeModified);
        nbt.setString("IconData", this.iconData);

        this.writeBlockPosListToNBT(this.subRegionSizes, nbt, "RegionSizes");
        this.writeBlockPosListToNBT(this.subRegionPositions, nbt, "RegionPositions");

        this.writeEntitiesToNBT(nbt, "Entities");
        this.writeTileEntitiesToNBT(nbt, "TileEntities");
        this.writeBlocksToNBT(nbt, "Blocks");

        return nbt;
    }

    private void writeBlockPosListToNBT(List<BlockPos> list, NBTTagCompound nbt, String tagName)
    {
        if (list.size() > 0)
        {
            NBTTagList tagList = new NBTTagList();

            for (BlockPos pos : list)
            {
                NBTTagCompound tag = new NBTTagCompound();
                this.writeBlockPosToTag(pos, tag);
                tagList.appendTag(tag);
            }

            nbt.setTag(tagName, tagList);
        }
    }

    private void writeEntitiesToNBT(NBTTagCompound nbt, String tagName)
    {
        if (this.entities.size() > 0)
        {
            NBTTagList tagList = new NBTTagList();

            for (EntityInfo info : this.entities)
            {
                NBTTagCompound tag = new NBTTagCompound();

                this.writeBlockPosToTag(info.pos, tag);
                this.writeVec3dToTag(info.posVec, tag);
                tag.setTag("EntityData", info.nbt);

                tagList.appendTag(tag);
            }

            nbt.setTag(tagName, tagList);
        }
    }

    private void writeTileEntitiesToNBT(NBTTagCompound nbt, String tagName)
    {
        if (this.tileEntities.size() > 0)
        {
            NBTTagList tagList = new NBTTagList();

            for (Map.Entry<BlockPos, NBTTagCompound> entry : this.tileEntities.entrySet())
            {
                NBTTagCompound tag = new NBTTagCompound();
                NBTTagCompound tileNbt = entry.getValue();
                tileNbt.removeTag("x");
                tileNbt.removeTag("y");
                tileNbt.removeTag("z");
                tag.setTag("TileNBT", entry.getValue());

                // Note: This within-schematic relative position is not inside the tile tag!
                this.writeBlockPosToTag(entry.getKey(), tag);

                tagList.appendTag(tag);
            }

            nbt.setTag(tagName, tagList);
        }
    }

    private void writeBlocksToNBT(NBTTagCompound nbt, String tagName)
    {
        if (this.blockContainers.size() > 0)
        {
            NBTTagCompound wrapper = new NBTTagCompound();
            int area = 0;

            for (LitematicaBlockStateContainer container : this.blockContainers)
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("Palette", container.getPalette().writeToNBT());
                tag.setIntArray("BlockStatesIntArray", this.longArrayToIntArray(container.getBackingLongArray()));
                wrapper.setTag("Area_" + area, tag);
                area++;
            }

            nbt.setTag(tagName, wrapper);
        }
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

    private void writeBlockPosToTag(BlockPos pos, NBTTagCompound tag)
    {
        tag.setInteger("x", pos.getX());
        tag.setInteger("y", pos.getY());
        tag.setInteger("z", pos.getZ());
    }

    private void writeVec3dToTag(Vec3d vec, NBTTagCompound tag)
    {
        tag.setDouble("dx", vec.x);
        tag.setDouble("dy", vec.y);
        tag.setDouble("dz", vec.z);
    }

    public boolean writeToFile(File dir, String filename, boolean override, IStringConsumer feedback)
    {
        if (filename.endsWith(".litematic") == false)
        {
            filename = filename + ".litematic";
        }

        File file = new File(dir, filename);

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (file.exists() && override == false)
            {
                feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exists", file.getAbsolutePath()));
                return false;
            }

            FileOutputStream os = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(this.writeToNBT(), os);
            os.close();
            return true;
        }
        catch (Exception e)
        {
            feedback.setString(I18n.format("litematica.error.schematic_write_to_file_failed.exception", file.getAbsolutePath()));
        }

        return false;
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
