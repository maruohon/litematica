package fi.dy.masa.litematica.scheduler.tasks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ArrayListMultimap;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.util.LayerRange;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

public abstract class TaskCountBlocksBase extends TaskBase
{
    protected final ArrayListMultimap<ChunkPos, StructureBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final Object2IntOpenHashMap<IBlockState> countsTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<IBlockState> countsMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<IBlockState> countsMismatch = new Object2IntOpenHashMap<>();
    protected final Set<ChunkPos> requiredChunks = new HashSet<>();
    protected final IMaterialList materialList;
    protected final World worldClient;
    protected final LayerRange layerRange;
    protected boolean finished;

    protected TaskCountBlocksBase(IMaterialList materialList)
    {
        super();

        this.materialList = materialList;
        this.worldClient = this.mc.world;

        if (materialList.getMaterialListType() == BlockInfoListType.ALL)
        {
            this.layerRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
        }
        else
        {
            this.layerRange = DataManager.getRenderLayerRange();
        }

        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute()
    {
        if (this.worldClient != null)
        {
            Iterator<ChunkPos> iter = this.requiredChunks.iterator();
            int processed = 0;

            while (iter.hasNext())
            {
                ChunkPos pos = iter.next();

                // All neighbor chunks loaded
                if (this.areSurroundingChunksLoaded(pos, this.worldClient, 1))
                {
                    this.countBlocksInChunk(pos);
                    iter.remove();
                    processed++;
                }
            }

            if (processed > 0)
            {
                this.updateInfoHudLinesMissingChunks(this.requiredChunks);
            }
        }

        this.finished = this.requiredChunks.isEmpty();

        return this.finished;
    }

    protected void addBoxesPerChunks(Collection<Box> allBoxes)
    {
        this.boxesInChunks.clear();
        this.requiredChunks.clear();

        this.requiredChunks.addAll(PositionUtils.getTouchedChunksForBoxes(allBoxes));

        for (ChunkPos pos : this.requiredChunks)
        {
            this.boxesInChunks.putAll(pos, PositionUtils.getBoxesWithinChunk(pos.x, pos.z, allBoxes));
        }
    }

    protected List<StructureBoundingBox> getBoxesInChunk(ChunkPos pos)
    {
        return this.boxesInChunks.get(pos);
    }

    protected void countBlocksInChunk(ChunkPos pos)
    {
        LayerRange range = this.layerRange;
        EnumFacing.Axis axis = range.getAxis();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (StructureBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == EnumFacing.Axis.X ? Math.max(bb.minX, range.getLayerMin()) : bb.minX;
            final int startY = axis == EnumFacing.Axis.Y ? Math.max(bb.minY, range.getLayerMin()) : bb.minY;
            final int startZ = axis == EnumFacing.Axis.Z ? Math.max(bb.minZ, range.getLayerMin()) : bb.minZ;
            final int endX = axis == EnumFacing.Axis.X ? Math.min(bb.maxX, range.getLayerMax()) : bb.maxX;
            final int endY = axis == EnumFacing.Axis.Y ? Math.min(bb.maxY, range.getLayerMax()) : bb.maxY;
            final int endZ = axis == EnumFacing.Axis.Z ? Math.min(bb.maxZ, range.getLayerMax()) : bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.setPos(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected abstract void countAtPosition(BlockPos pos);

    @Override
    public void stop()
    {
        if (this.finished && this.mc.player != null)
        {
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    this.countsTotal, this.countsMissing, this.countsMismatch, this.mc.player);
            this.materialList.setMaterialListEntries(list);
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);
    }
}
