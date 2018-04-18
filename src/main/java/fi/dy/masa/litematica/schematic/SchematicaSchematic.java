package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.util.Constants;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.template.PlacementSettings;

public class SchematicaSchematic
{
    private BlockPos size = BlockPos.ORIGIN;
    private IBlockState[] blocks;
    private IBlockState[] palette;
    private Map<BlockPos, NBTTagCompound> tiles = new HashMap<>();
    private List<NBTTagCompound> entities = new ArrayList<>();
    private String fileName;

    private SchematicaSchematic()
    {
    }

    public BlockPos getSize()
    {
        return this.size;
    }

    public IBlockState[] getBlocks()
    {
        return this.blocks;
    }

    public void addBlocksToWorld(World world, BlockPos posStart, PlacementSettings placement, int flags)
    {
        final int width = this.size.getX();
        final int height = this.size.getY();
        final int length = this.size.getZ();
        final int numBlocks = width * height * length;

        if (this.blocks != null && this.blocks.length == numBlocks && numBlocks > 0)
        {
            Block ignoredBlock = placement.getReplacedBlock();
            BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos(0, 0, 0);
            int index = 0;

            // Place blocks and read any TileEntity data
            for (int y = 0; y < height; ++y)
            {
                for (int z = 0; z < length; ++z)
                {
                    for (int x = 0; x < width; ++x, index++)
                    {
                        IBlockState state = this.blocks[index];

                        if (ignoredBlock != null && state.getBlock() == ignoredBlock)
                        {
                            continue;
                        }

                        posMutable.setPos(x, y, z);
                        NBTTagCompound teNBT = this.tiles.get(posMutable);

                        transformBlockPos(placement, posMutable);
                        posMutable.setPos(  posMutable.getX() + posStart.getX(),
                                            posMutable.getY() + posStart.getY(),
                                            posMutable.getZ() + posStart.getZ());

                        state = state.withMirror(placement.getMirror());
                        state = state.withRotation(placement.getRotation());

                        if (teNBT != null)
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                if (te instanceof IInventory)
                                {
                                    ((IInventory) te).clear();
                                }

                                world.setBlockState(posMutable, Blocks.BARRIER.getDefaultState(), 4);
                            }
                        }

                        if (world.setBlockState(posMutable, state, flags) && teNBT != null)
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                teNBT.setInteger("x", posMutable.getX());
                                teNBT.setInteger("y", posMutable.getY());
                                teNBT.setInteger("z", posMutable.getZ());
                                te.readFromNBT(teNBT);
                                te.mirror(placement.getMirror());
                                te.rotate(placement.getRotation());
                            }
                        }
                    }
                }
            }

            // Update blocks
            for (int y = 0; y < height; ++y)
            {
                for (int z = 0; z < length; ++z)
                {
                    for (int x = 0; x < width; ++x)
                    {
                        posMutable.setPos(x, y, z);
                        NBTTagCompound teNBT = this.tiles.get(posMutable);

                        transformBlockPos(placement, posMutable);
                        posMutable.setPos(  posMutable.getX() + posStart.getX(),
                                            posMutable.getY() + posStart.getY(),
                                            posMutable.getZ() + posStart.getZ());

                        world.notifyNeighborsRespectDebug(posMutable, world.getBlockState(posMutable).getBlock(), false);

                        if (teNBT != null)
                        {
                            TileEntity te = world.getTileEntity(posMutable);

                            if (te != null)
                            {
                                te.markDirty();
                            }
                        }
                    }
                }
            }

            if (placement.getIgnoreEntities() == false)
            {
                this.addEntitiesToWorld(world, posStart, placement.getMirror(), placement.getRotation());
            }
        }
    }

    private void addEntitiesToWorld(World worldIn, BlockPos pos, Mirror mirrorIn, Rotation rotationIn)
    {
        /*
        for (Template.EntityInfo template$entityinfo : this.entities)
        {
            BlockPos blockpos = transformedBlockPos(template$entityinfo.blockPos, mirrorIn, rotationIn).add(pos);

            NBTTagCompound nbttagcompound = template$entityinfo.entityData;
            Vec3d vec3d = transformedVec3d(template$entityinfo.pos, mirrorIn, rotationIn);
            Vec3d vec3d1 = vec3d.addVector((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
            NBTTagList nbttaglist = new NBTTagList();
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.x));
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.y));
            nbttaglist.appendTag(new NBTTagDouble(vec3d1.z));
            nbttagcompound.setTag("Pos", nbttaglist);
            nbttagcompound.setUniqueId("UUID", UUID.randomUUID());
            Entity entity;

            try
            {
                entity = EntityList.createEntityFromNBT(nbttagcompound, worldIn);
            }
            catch (Exception var15)
            {
                entity = null;
            }

            if (entity != null)
            {
                float f = entity.getMirroredYaw(mirrorIn);
                f = f + (entity.rotationYaw - entity.getRotatedYaw(rotationIn));
                entity.setLocationAndAngles(vec3d1.x, vec3d1.y, vec3d1.z, f, entity.rotationPitch);
                worldIn.spawnEntity(entity);
            }
        }
        */
    }

    @Nullable
    public static SchematicaSchematic createFromFile(File file)
    {
        SchematicaSchematic schematic = new SchematicaSchematic();

        if (schematic.readFromFile(file))
        {
            return schematic;
        }

        return null;
    }

    public boolean readFromNBT(NBTTagCompound nbt)
    {
        // This method was implemented based on
        // https://minecraft.gamepedia.com/Schematic_file_format
        // as it was on 2018-04-18.
        if (nbt.hasKey("Blocks", Constants.NBT.TAG_BYTE_ARRAY) &&
            nbt.hasKey("Data", Constants.NBT.TAG_BYTE_ARRAY))
        {
            int width = nbt.getShort("Width");
            int height = nbt.getShort("Height");
            int length = nbt.getShort("Length");
            byte[] blockIdsByte = nbt.getByteArray("Blocks");
            byte[] meta = nbt.getByteArray("Data");
            final int numBlocks = blockIdsByte.length;

            this.size = new BlockPos(width, height, length);

            if (numBlocks != (width * height * length))
            {
                LiteModLitematica.logger.error("Schematic: Mismatched block array size compared to the width/height/length, blocks: {}, W x H x L: {} x {} x {}",
                        numBlocks, width, height, length);
                return false;
            }

            if (numBlocks != meta.length)
            {
                LiteModLitematica.logger.error("Schematic: Mismatched block ID and metadata array sizes, blocks: {}, meta: {}", numBlocks, meta.length);
                return false;
            }

            if (this.readPalette(nbt) == false)
            {
                LiteModLitematica.logger.error("Schematic: Failed to read the block palette");
                return false;
            }

            this.blocks = new IBlockState[numBlocks];

            if (nbt.hasKey("AddBlocks", Constants.NBT.TAG_BYTE_ARRAY))
            {
                byte[] add = nbt.getByteArray("AddBlocks");

                if (add.length != (blockIdsByte.length / 2))
                {
                    LiteModLitematica.logger.error("Schematic: Add array size mismatch, blocks: {}, add: {}, expected add: {}",
                            numBlocks, add.length, numBlocks / 2);
                    return false;
                }

                final int loopMax;

                // Even number of blocks, we can 
                if ((numBlocks % 2) == 0)
                {
                    loopMax = numBlocks - 1;
                }
                else
                {
                    loopMax = numBlocks - 2;
                }

                // Handle two positions per iteration, ie. one full byte of the add array
                for (int bi = 0, ai = 0; bi < loopMax; bi += 2, ai++)
                {
                    byte addValue = add[ai];
                    this.blocks[bi    ] = this.palette[((addValue & 0xF0) << 4) | (((int) blockIdsByte[bi]) & 0xFF)];
                    this.blocks[bi + 1] = this.palette[((addValue & 0x0F) << 8) | (((int) blockIdsByte[bi]) & 0xFF)];
                }

                // Odd number of blocks, handle the last position
                if ((numBlocks % 2) != 0)
                {
                    this.blocks[numBlocks - 1] = this.palette[((add[numBlocks / 2] & 0xF0) << 4) | (((int) blockIdsByte[numBlocks - 1]) & 0xFF)];
                }
            }
            // Old Schematica format
            else if (nbt.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
            {
                // FIXME is this array 4 or 8 bits per block?
                LiteModLitematica.logger.error("Schematic: Old Schematica format detected, not currently implemented...");
                return false;
            }
            else
            {
                for (int i = 0; i < numBlocks; i++)
                {
                    this.blocks[i] = this.palette[((int) blockIdsByte[i]) & 0xFF];
                }
            }

            this.readEntities(nbt);
            this.readTileEntities(nbt);

            return true;
        }
        else
        {
            LiteModLitematica.logger.error("Schematic: Missing block data in the schematic '{}'", this.fileName);
        }

        return false;
    }

    private boolean readPalette(NBTTagCompound nbt)
    {
        final IBlockState air = Blocks.AIR.getDefaultState();
        this.palette = new IBlockState[4096];
        Arrays.fill(this.palette, air);

        // Schematica palette
        if (nbt.hasKey("SchematicaMapping", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound tag = nbt.getCompoundTag("SchematicaMapping");
            Set<String> keys = tag.getKeySet();

            for (String key : keys)
            {
                int id = tag.getShort(key);

                if (id >= this.palette.length)
                {
                    LiteModLitematica.logger.error("Schematic: Invalid ID '{}' in SchematicaMapping for block '{}', max = 4095", id, key);
                    return false;
                }

                Block block = Block.REGISTRY.getObject(new ResourceLocation(key));

                if (block != null)
                {
                    this.palette[id] = block.getDefaultState();
                }
                else
                {
                    LiteModLitematica.logger.error("Schematic: Missing/non-existing block '{}' in SchematicaMapping", key);
                }
            }
        }
        // MCEdit2 palette
        else if (nbt.hasKey("BlockIDs", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound tag = nbt.getCompoundTag("BlockIDs");
            Set<String> keys = tag.getKeySet();

            for (String idStr : keys)
            {
                String key = tag.getString(idStr);
                int id;

                try
                {
                    id = Integer.parseInt(idStr);
                }
                catch (NumberFormatException e)
                {
                    LiteModLitematica.logger.error("Schematic: Invalid ID '{}' (not a number) in MCEdit2 palette for block '{}'", idStr, key);
                    continue;
                }

                if (id >= this.palette.length)
                {
                    LiteModLitematica.logger.error("Schematic: Invalid ID '{}' in MCEdit2 palette for block '{}', max = 4095", id, key);
                    return false;
                }

                Block block = Block.REGISTRY.getObject(new ResourceLocation(key));

                if (block != null)
                {
                    this.palette[id] = block.getDefaultState();
                }
                else
                {
                    LiteModLitematica.logger.error("Schematic: Missing/non-existing block '{}' in MCEdit2 palette", key);
                }
            }
        }
        // No palette, use the current registry IDs directly
        else
        {
            for (ResourceLocation key : Block.REGISTRY.getKeys())
            {
                Block block = Block.REGISTRY.getObject(key);

                if (block != null)
                {
                    int id = Block.getIdFromBlock(block);

                    if (id >= 0 && id < this.palette.length)
                    {
                        this.palette[id] = block.getDefaultState();
                    }
                    else
                    {
                        LiteModLitematica.logger.error("Schematic: Invalid ID {} for block '{}' from the registry", id, key);
                    }
                }
            }
        }

        return true;
    }

    private void readEntities(NBTTagCompound nbt)
    {
        this.entities.clear();
        NBTTagList tagList = nbt.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < tagList.tagCount(); ++i)
        {
            this.entities.add(tagList.getCompoundTagAt(i));

            /*
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagList posList = tag.getTagList("pos", 6);
            Vec3d vec3d = new Vec3d(posList.getDoubleAt(0), posList.getDoubleAt(1), posList.getDoubleAt(2));
            NBTTagList blockPosList = tag.getTagList("blockPos", 3);
            BlockPos blockpos1 = new BlockPos(blockPosList.getIntAt(0), blockPosList.getIntAt(1), blockPosList.getIntAt(2));

            if (tag.hasKey("nbt"))
            {
                NBTTagCompound nbttagcompound2 = tag.getCompoundTag("nbt");
                this.entities.add(new Template.EntityInfo(vec3d, blockpos1, nbttagcompound2));
            }
            */
        }
    }

    private void readTileEntities(NBTTagCompound nbt)
    {
        this.tiles.clear();
        NBTTagList tagList = nbt.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < tagList.tagCount(); ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
            this.tiles.put(pos, tag);
        }
    }

    public boolean readFromFile(File file)
    {
        if (file.exists() && file.isFile() && file.canRead())
        {
            this.fileName = file.getName();

            try
            {
                FileInputStream is = new FileInputStream(file);
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                is.close();

                return this.readFromNBT(nbt);
            }
            catch (IOException e)
            {
                LiteModLitematica.logger.error("Schematic: Failed to read Schematic data from file '{}'", file.getAbsolutePath());
            }
        }

        return false;
    }

    public static void transformBlockPos(PlacementSettings placement, BlockPos.MutableBlockPos pos)
    {
        transformBlockPos(pos, placement.getMirror(), placement.getRotation());
    }

    public static void transformBlockPos(BlockPos.MutableBlockPos pos, Mirror mirror, Rotation rotation)
    {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        boolean isMirrored = true;

        switch (mirror)
        {
            // LEFT_RIGHT is essentially NORTH_SOUTH
            case LEFT_RIGHT:
                z = -z;
                break;
            // FRONT_BACK is essentially EAST_WEST
            case FRONT_BACK:
                x = -x;
                break;
            default:
                isMirrored = false;
        }

        switch (rotation)
        {
            case CLOCKWISE_90:
                pos.setPos(-z, y,  x);
            case COUNTERCLOCKWISE_90:
                pos.setPos( z, y, -x);
            case CLOCKWISE_180:
                pos.setPos(-x, y, -z);
            default:
                if (isMirrored)
                {
                    pos.setPos(x, y, z);
                }
        }
    }
}
