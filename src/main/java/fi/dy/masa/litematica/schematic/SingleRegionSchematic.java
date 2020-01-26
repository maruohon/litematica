package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.LiteModLitematica;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public abstract class SingleRegionSchematic extends SchematicBase implements ISchematicRegion
{
    protected final Map<BlockPos, NBTTagCompound> blockEntities = new HashMap<>();
    protected final Map<BlockPos, NextTickListEntry> pendingBlockTicks = new HashMap<>();
    protected final List<EntityInfo> entities = new ArrayList<>();
    protected ILitematicaBlockStateContainer blockContainer;
    protected BlockPos regionPos = BlockPos.ORIGIN;
    private Vec3i regionSize = Vec3i.NULL_VECTOR;

    public SingleRegionSchematic(File file)
    {
        super(file);

        this.getMetadata().setRegionCount(1);
    }

    @Override
    public void clear()
    {
        this.blockEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
        this.metadata.clearModifiedSinceSaved();
    }

    @Override
    public int getSubRegionCount()
    {
        return 1;
    }

    @Override
    public ImmutableList<String> getRegionNames()
    {
        return ImmutableList.of(this.getMetadata().getName());
    }

    @Override
    public ImmutableMap<String, ISchematicRegion> getRegions()
    {
        return ImmutableMap.of(this.getMetadata().getName(), this);
    }

    @Override
    public ISchematicRegion getSchematicRegion(String regionName)
    {
        return this;
    }

    @Override
    public BlockPos getPosition()
    {
        return this.regionPos;
    }

    @Override
    public Vec3i getSize()
    {
        return this.regionSize;
    }

    protected void setSize(@Nullable Vec3i size, boolean createBlockContainer)
    {
        this.regionSize = size;

        if (size != null)
        {
            if (isSizeValid(size) == false)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.invalid_or_missing_size_value", size.getX(), size.getY(), size.getZ());
                return;
            }

            if (createBlockContainer)
            {
                this.createEmptyContainer(size);
            }

            this.getMetadata().setEnclosingSize(size);
            this.getMetadata().setTotalVolume(PositionUtils.getAreaVolume(size));
        }
    }

    protected void createEmptyContainer(Vec3i size)
    {
        this.blockContainer = new LitematicaBlockStateContainerFull(size);
    }

    @Override
    public ILitematicaBlockStateContainer getBlockStateContainer()
    {
        return this.blockContainer;
    }

    @Override
    public Map<BlockPos, NBTTagCompound> getBlockEntityMap()
    {
        return this.blockEntities;
    }

    @Override
    public List<EntityInfo> getEntityList()
    {
        return this.entities;
    }

    @Override
    public Map<BlockPos, NextTickListEntry> getBlockTickMap()
    {
        return this.pendingBlockTicks;
    }

    @Override
    public Vec3i getEnclosingSize()
    {
        return this.getSize();
    }

    @Override
    public void readFrom(ISchematic other)
    {
        ImmutableMap<String, ISchematicRegion> regions = other.getRegions();

        if (regions.isEmpty() == false)
        {
            this.clear();

            Vec3i size = other.getEnclosingSize();

            if (size != null)
            {
                try
                {
                    this.setSize(size, regions.size() > 1);
                    this.readFrom(regions);
                }
                catch (Exception e)
                {
                    LiteModLitematica.logger.warn("Exception while reading schematic contents from another schematic", e);
                }
            }
        }

        this.getMetadata().copyFrom(other.getMetadata());
        this.getMetadata().setRegionCount(1);
    }

    protected void readFrom(ImmutableMap<String, ISchematicRegion> regions)
    {
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCornersForRegions(regions.values());

        if (pair == null)
        {
            return;
        }

        final BlockPos minCorner = pair.getLeft();

        if (regions.size() == 1)
        {
            for (ISchematicRegion region : regions.values())
            {
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();
                Vec3i size = containerOther.getSize();

                if (this.getContainerClass() == containerOther.getClass())
                {
                    this.blockContainer = containerOther.copy();
                }
                else
                {
                    this.createEmptyContainer(size);
                    this.copyContainerContents(containerOther, this.blockContainer);
                }
            }
        }
        else
        {
            for (ISchematicRegion region : regions.values())
            {
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();
                ILitematicaBlockStateContainer containerThis = this.blockContainer;
                Vec3i size = containerOther.getSize();

                // This is the relative position of this sub-region within the new single region enclosing schematic volume
                Vec3i regionOffset = this.getRegionOffset(region, minCorner);

                final int sizeX = size.getX();
                final int sizeY = size.getY();
                final int sizeZ = size.getZ();
                final int offX = regionOffset.getX();
                final int offY = regionOffset.getY();
                final int offZ = regionOffset.getZ();

                for (int y = 0; y < sizeY; ++y)
                {
                    for (int z = 0; z < sizeZ; ++z)
                    {
                        for (int x = 0; x < sizeX; ++x)
                        {
                            IBlockState state = containerOther.getBlockState(x, y, z);
                            containerThis.setBlockState(x + offX, y + offY, z + offZ, state);
                        }
                    }
                }
            }
        }

        for (ISchematicRegion region : regions.values())
        {
            // No offset for this sub-region, use the positions in the maps without modifications
            if (region.getPosition().equals(BlockPos.ORIGIN))
            {
                region.getBlockEntityMap().entrySet().forEach((entry) -> this.blockEntities.put(entry.getKey(), entry.getValue().copy()));
                region.getBlockTickMap().entrySet().forEach((entry) -> this.pendingBlockTicks.put(entry.getKey(), entry.getValue()));
                region.getEntityList().forEach((info) -> this.entities.add(info.copy()));
            }
            else
            {
                // This is the relative position of this sub-region within the new single region enclosing schematic volume
                Vec3i regionOffsetBlocks = this.getRegionOffset(region, minCorner);

                region.getBlockEntityMap().entrySet().forEach((entry) -> {
                    BlockPos pos = entry.getKey().add(regionOffsetBlocks);
                    this.blockEntities.put(pos, entry.getValue().copy());
                });

                region.getBlockTickMap().entrySet().forEach((entry) -> {
                    BlockPos pos = entry.getKey().add(regionOffsetBlocks);
                    this.pendingBlockTicks.put(pos, entry.getValue());
                });

                // The entity positions are not relative to the sub-region's minimum corner,
                // but to the sub-region's origin point, whichever corner that is at.
                Vec3i regionOffsetEntities = region.getPosition().subtract(minCorner);

                region.getEntityList().forEach((info) -> {
                    Vec3d pos = info.pos.add(regionOffsetEntities.getX(), regionOffsetEntities.getY(), regionOffsetEntities.getZ());
                    NBTTagCompound nbt = info.nbt.copy();
                    NBTUtils.writeVec3dToListTag(pos, nbt);
                    this.entities.add(new EntityInfo(pos, nbt));
                });
            }
        }
    }

    protected Vec3i getRegionOffset(ISchematicRegion region, BlockPos minCorner)
    {
        // Get the offset from the region's block state container origin
        // (the minimum corner of the region) to the enclosing area's origin/minimum corner.
        BlockPos regionPos = region.getPosition();
        Vec3i endRel = PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize());
        BlockPos regionEnd = regionPos.add(endRel);
        BlockPos regionMin = PositionUtils.getMinCorner(regionPos, regionEnd);
        BlockPos regionOffset = regionMin.subtract(minCorner);

        return regionOffset;
    }

    @Override
    public final boolean fromTag(NBTTagCompound tag)
    {
        this.clear();

        this.initFromTag(tag);
        this.setSize(this.readSizeFromTag(tag), true);

        if (isSizeValid(this.regionSize) == false)
        {
            InfoUtils.printErrorMessage("litematica.message.error.schematic_read.invalid_or_missing_size", this.getFile().getAbsolutePath());
            return false;
        }

        if (this.readBlocksFromTag(tag))
        {
            this.blockEntities.putAll(this.readBlockEntitiesFromTag(tag));
            this.entities.addAll(this.readEntitiesFromTag(tag));
            this.readMetadataFromTag(tag);

            return true;
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.message.error.schematic_read.missing_or_invalid_data", this.getFile().getAbsolutePath());
            return false;
        }
    }

    /**
     * This method is called first, when reading data from NBT.
     * It allows the schematic to initialize any required custom things before the common methods are called.
     * @param tag
     */
    protected void initFromTag(NBTTagCompound tag)
    {
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        // Not stored in metadata yet
        if (this.getMetadata().getTotalBlocks() < 0)
        {
            long totalBlocks = this.blockContainer.getTotalBlockCount();
            this.getMetadata().setTotalBlocks(totalBlocks);
        }
    }

    @Nullable protected abstract Vec3i readSizeFromTag(NBTTagCompound tag);

    protected abstract boolean readBlocksFromTag(NBTTagCompound tag);

    protected abstract Map<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag);

    protected abstract List<EntityInfo> readEntitiesFromTag(NBTTagCompound tag);
}
