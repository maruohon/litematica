package fi.dy.masa.litematica.schematic;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;

public interface ISchematicRegion
{
    /**
     * Returns the relative position of this region in relation to the origin of the entire schematic.
     * @return
     */
    BlockPos getPosition();

    /**
     * Returns the size of this region.
     * <b>Note:</b> The size can be negative, if the second corner is on the negative side
     * on any axis compared to the primary/origin corner.
     * @return
     */
    Vec3i getSize();

    /**
     * Returns the block state container used for storing the block states in this region
     * @return
     */
    LitematicaBlockStateContainer getBlockStateContainer();

    /**
     * Returns the BlockEntity map used for this region
     * @return
     */
    Map<BlockPos, NBTTagCompound> getBlockEntityMap();

    /**
     * Returns the entity list for this region
     * @return
     */
    List<EntityInfo> getEntityList();

    /*
     * Returns the map for the scheduled Block ticks in this region
     */
    Map<BlockPos, NextTickListEntry> getBlockTickMap();
}
