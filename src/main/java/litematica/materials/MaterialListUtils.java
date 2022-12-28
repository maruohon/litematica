package litematica.materials;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;

import malilib.gui.BaseScreen;
import malilib.gui.StringListSelectionScreen;
import malilib.util.data.ItemType;
import malilib.util.game.wrap.GameUtils;
import malilib.util.game.wrap.ItemWrap;
import malilib.util.inventory.InventoryUtils;
import litematica.config.Configs;
import litematica.data.DataManager;
import litematica.gui.MaterialListScreen;
import litematica.schematic.ISchematic;
import litematica.schematic.ISchematicRegion;
import litematica.schematic.container.ILitematicaBlockStateContainer;

public class MaterialListUtils
{
    public static List<MaterialListEntry> createMaterialListFor(ISchematic schematic)
    {
        return createMaterialListFor(schematic, schematic.getRegionNames());
    }

    public static List<MaterialListEntry> createMaterialListFor(ISchematic schematic, Collection<String> subRegions)
    {
        Object2LongOpenHashMap<IBlockState> countsTotal = new Object2LongOpenHashMap<>();

        for (String regionName : subRegions)
        {
            ISchematicRegion region = schematic.getSchematicRegion(regionName);
            ILitematicaBlockStateContainer container = region != null ? region.getBlockStateContainer() : null;

            if (container != null)
            {
                if (Configs.Generic.MATERIALS_FROM_CONTAINER.getBooleanValue())
                {
                    for (Map.Entry<IBlockState, Long> entry : container.getBlockCountsMap().entrySet())
                    {
                        long total = entry.getValue().longValue();

                        // Don't include stale entries from the palette due to Rebuild operations etc.
                        if (total > 0)
                        {
                            countsTotal.addTo(entry.getKey(), (int) total);
                        }
                    }
                }
                else
                {
                    Vec3i size = container.getSize();
                    final int sizeX = size.getX();
                    final int sizeY = size.getY();
                    final int sizeZ = size.getZ();

                    for (int y = 0; y < sizeY; ++y)
                    {
                        for (int z = 0; z < sizeZ; ++z)
                        {
                            for (int x = 0; x < sizeX; ++x)
                            {
                                IBlockState state = container.getBlockState(x, y, z);
                                countsTotal.addTo(state, 1);
                            }
                        }
                    }
                }
            }
        }

        return getMaterialList(countsTotal, countsTotal, new Object2LongOpenHashMap<>());
    }

    public static List<MaterialListEntry> getMaterialList(
            Object2LongOpenHashMap<IBlockState> countsTotal,
            Object2LongOpenHashMap<IBlockState> countsMissing,
            Object2LongOpenHashMap<IBlockState> countsMismatch)
    {
        List<MaterialListEntry> list = new ArrayList<>();

        if (countsTotal.isEmpty() == false)
        {
            MaterialCache cache = MaterialCache.getInstance();
            Object2LongOpenHashMap<ItemType> itemTypesTotal = new Object2LongOpenHashMap<>();
            Object2LongOpenHashMap<ItemType> itemTypesMissing = new Object2LongOpenHashMap<>();
            Object2LongOpenHashMap<ItemType> itemTypesMismatch = new Object2LongOpenHashMap<>();

            convertStatesToStacks(countsTotal, itemTypesTotal, cache);
            convertStatesToStacks(countsMissing, itemTypesMissing, cache);
            convertStatesToStacks(countsMismatch, itemTypesMismatch, cache);

            IInventory playerInv = GameUtils.getPlayerInventory();
            Object2IntOpenHashMap<ItemType> playerInvItems = playerInv != null ? InventoryUtils.getInventoryItemCounts(playerInv) : new Object2IntOpenHashMap<>();

            for (ItemType type : itemTypesTotal.keySet())
            {
                list.add(new MaterialListEntry(type.getStack().copy(),
                        itemTypesTotal.getLong(type),
                        itemTypesMissing.getLong(type),
                        itemTypesMismatch.getLong(type),
                        playerInvItems.getInt(type)));
            }
        }

        return list;
    }

    private static void convertStatesToStacks(
            Object2LongOpenHashMap<IBlockState> blockStatesIn,
            Object2LongOpenHashMap<ItemType> itemTypesOut,
            MaterialCache cache)
    {
        // Convert from counts per IBlockState to counts per different stacks
        for (IBlockState state : blockStatesIn.keySet())
        {
            if (cache.requiresMultipleItems(state))
            {
                for (ItemStack stack : cache.getItems(state))
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesOut.addTo(type, blockStatesIn.getLong(state) * stack.getCount());
                }
            }
            else
            {
                ItemStack stack = cache.getRequiredBuildItemForState(state);

                if (ItemWrap.notEmpty(stack))
                {
                    ItemType type = new ItemType(stack, false, true);
                    itemTypesOut.addTo(type, blockStatesIn.getLong(state) * stack.getCount());
                }
            }
        }
    }

    public static void updateAvailableCounts(List<MaterialListEntry> list)
    {
        Object2IntOpenHashMap<ItemType> playerInvItems = InventoryUtils.getInventoryItemCounts(GameUtils.getClientPlayer().inventory);

        for (MaterialListEntry entry : list)
        {
            ItemType type = new ItemType(entry.getStack(), false, true);
            int countAvailable = playerInvItems.getInt(type);
            entry.setCountAvailable(countAvailable);
        }
    }

    public static void openMaterialListScreenFor(ISchematic schematic)
    {
        if (BaseScreen.isShiftDown())
        {
            StringListSelectionScreen screen = new StringListSelectionScreen(schematic.getRegionNames(),
                                                    (strings) -> createMaterialListOfRegions(schematic, strings));
            screen.setTitle("litematica.title.screen.material_list.select_schematic_regions", schematic.getMetadata().getName());
            BaseScreen.openScreenWithParent(screen);
        }
        else
        {
            MaterialListSchematic materialList = new MaterialListSchematic(schematic, true);
            DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
            BaseScreen.openScreen(new MaterialListScreen(materialList));
        }
    }

    public static void createMaterialListOfRegions(ISchematic schematic, Collection<String> regions)
    {
        MaterialListSchematic materialList = new MaterialListSchematic(schematic, regions, true);
        DataManager.setMaterialList(materialList); // Remember the last opened material list for the hotkey to (re-) open it
        BaseScreen.openScreen(new MaterialListScreen(materialList));
    }
}
