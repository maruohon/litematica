package fi.dy.masa.litematica.scheduler.tasks;

import java.util.List;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.malilib.util.position.LayerRange;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.world.SchematicWorldRenderingNotifier;

public abstract class TaskCountBlocksMaterialList extends TaskCountBlocksBase
{
    protected final Object2LongOpenHashMap<IBlockState> countsMissing = new Object2LongOpenHashMap<>();
    protected final Object2LongOpenHashMap<IBlockState> countsMismatch = new Object2LongOpenHashMap<>();
    protected final IMaterialList materialList;
    protected final LayerRange layerRange;

    protected TaskCountBlocksMaterialList(IMaterialList materialList, String nameOnHud)
    {
        super(nameOnHud);

        this.materialList = materialList;

        if (materialList.getMaterialListType() == BlockInfoListType.ALL)
        {
            this.layerRange = new LayerRange(SchematicWorldRenderingNotifier.INSTANCE);
        }
        else
        {
            this.layerRange = DataManager.getRenderLayerRange();
        }
    }

    @Override
    protected boolean processChunk(ChunkPos pos)
    {
        this.countBlocksInChunkRespectingLayerRange(pos, this.layerRange);
        return true;
    }

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
