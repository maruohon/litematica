package de.meinbuild.liteschem.scheduler.tasks;

import java.util.List;
import de.meinbuild.liteschem.data.DataManager;
import de.meinbuild.liteschem.materials.IMaterialList;
import de.meinbuild.liteschem.materials.MaterialListEntry;
import de.meinbuild.liteschem.materials.MaterialListUtils;
import de.meinbuild.liteschem.render.infohud.InfoHud;
import de.meinbuild.liteschem.util.BlockInfoListType;
import de.meinbuild.liteschem.util.SchematicWorldRefresher;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;

public abstract class TaskCountBlocksBase extends TaskProcessChunkBase
{
    protected final Object2IntOpenHashMap<BlockState> countsTotal = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMissing = new Object2IntOpenHashMap<>();
    protected final Object2IntOpenHashMap<BlockState> countsMismatch = new Object2IntOpenHashMap<>();
    protected final IMaterialList materialList;
    protected final LayerRange layerRange;

    protected TaskCountBlocksBase(IMaterialList materialList, String nameOnHud)
    {
        super(nameOnHud);

        this.materialList = materialList;

        if (materialList.getMaterialListType() == BlockInfoListType.ALL)
        {
            this.layerRange = new LayerRange(SchematicWorldRefresher.INSTANCE);
        }
        else
        {
            this.layerRange = DataManager.getRenderLayerRange();
        }
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        // All surrounding chunks are loaded on the client
        return this.areSurroundingChunksLoaded(pos, this.worldClient, 1);
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        this.countBlocksInChunk(pos);
        return true;
    }

    protected void countBlocksInChunk(ChunkPos pos)
    {
        LayerRange range = this.layerRange;
        Direction.Axis axis = range.getAxis();
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        for (IntBoundingBox bb : this.getBoxesInChunk(pos))
        {
            final int startX = axis == Direction.Axis.X ? Math.max(bb.minX, range.getLayerMin()) : bb.minX;
            final int startY = axis == Direction.Axis.Y ? Math.max(bb.minY, range.getLayerMin()) : bb.minY;
            final int startZ = axis == Direction.Axis.Z ? Math.max(bb.minZ, range.getLayerMin()) : bb.minZ;
            final int endX = axis == Direction.Axis.X ? Math.min(bb.maxX, range.getLayerMax()) : bb.maxX;
            final int endY = axis == Direction.Axis.Y ? Math.min(bb.maxY, range.getLayerMax()) : bb.maxY;
            final int endZ = axis == Direction.Axis.Z ? Math.min(bb.maxZ, range.getLayerMax()) : bb.maxZ;

            for (int y = startY; y <= endY; ++y)
            {
                for (int z = startZ; z <= endZ; ++z)
                {
                    for (int x = startX; x <= endX; ++x)
                    {
                        posMutable.set(x, y, z);
                        this.countAtPosition(posMutable);
                    }
                }
            }
        }
    }

    protected abstract void countAtPosition(BlockPos pos);

    @Override
    protected void onStop()
    {
        if (this.finished && this.mc.player != null)
        {
            List<MaterialListEntry> list = MaterialListUtils.getMaterialList(
                    this.countsTotal, this.countsMissing, this.countsMismatch, this.mc.player);
            this.materialList.setMaterialListEntries(list);
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }
}
