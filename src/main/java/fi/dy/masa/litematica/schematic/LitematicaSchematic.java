package fi.dy.masa.litematica.schematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.util.PositionUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class LitematicaSchematic
{
    private final List<BlockStateContainerVariable> blocks = new ArrayList<>();
    private Map<BlockPos, NBTTagCompound> tileEntities = new HashMap<>();
    private List<EntityInfo> entities = new ArrayList<>();
    private final List<BlockPos> subRegionPositions = new ArrayList<>();
    private final List<BlockPos> subRegionSizes = new ArrayList<>();
    private BlockPos totalSize = BlockPos.ORIGIN;
    private String author = "Unknown";
    private long timeCreated;
    private long timeModified;
    private IBlockState[] palette;

    public BlockPos getTotalSize()
    {
        return this.totalSize;
    }

    public String getAuthor()
    {
        return this.author;
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
            final int length = sizeX * sizeY * sizeZ;
            BlockStateContainerVariable container = new BlockStateContainerVariable(length);

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
                                BlockPos pos = new BlockPos(x + startX - origin.getX(), y + startY - origin.getY(), z + startZ - origin.getZ());
                                NBTTagCompound nbt = new NBTTagCompound();
                                nbt = te.writeToNBT(nbt);
                                this.tileEntities.put(pos, nbt);
                            }
                        }
                    }
                }
            }

            this.blocks.add(container);
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
