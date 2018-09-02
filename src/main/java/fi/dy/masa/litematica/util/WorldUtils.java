package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.StringUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

public class WorldUtils
{
    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(File inputDir, String inputFileName, IStringConsumer feedback)
    {
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(new File(inputDir, inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            return null;
        }

        WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
        WorldClient world = new WorldSchematic(null, settings, 0, EnumDifficulty.PEACEFUL, Minecraft.getMinecraft().mcProfiler);

        WorldUtils.loadChunksClientWorld(world, BlockPos.ORIGIN, schematic.getSize());
        PlacementSettings placementSettings = new PlacementSettings();
        placementSettings.setIgnoreEntities(true);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        area.getSelectedSubRegionBox().setPos2(new BlockPos(schematic.getSize()));

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, false, "?", feedback);

        if (litematicaSchematic != null)
        {
            litematicaSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        return litematicaSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(
            File structureDir, String structureFileName, File outputDir, String outputFileName, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(File structureDir, String structureFileName, IStringConsumer feedback)
    {
        DataFixer fixer = Minecraft.getMinecraft().getDataFixer();
        File file = new File(structureDir, structureFileName);

        try
        {
            InputStream is = new FileInputStream(file);
            Template template = readTemplateFromStream(is, fixer);
            is.close();

            WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
            WorldClient world = new WorldSchematic(null, settings, 0, EnumDifficulty.PEACEFUL, Minecraft.getMinecraft().mcProfiler);

            loadChunksClientWorld(world, BlockPos.ORIGIN, template.getSize());

            PlacementSettings placementSettings = new PlacementSettings();
            placementSettings.setIgnoreEntities(true);
            template.addBlocksToWorld(world, BlockPos.ORIGIN, null, placementSettings, 0x12);

            String subRegionName = FileUtils.getNameWithoutExtension(structureFileName) + " (Converted Structure)";
            AreaSelection area = new AreaSelection();
            area.setName(subRegionName);
            subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
            area.setSelectedSubRegionBox(subRegionName);
            area.getSelectedSubRegionBox().setPos2(template.getSize().add(-1, -1, -1));

            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, false, "?", feedback);

            if (litematicaSchematic != null)
            {
                //litematicaSchematic.takeEntityDataFromVanillaStructure(template, subRegionName); // TODO
            }
            else
            {
                feedback.setString("litematica.error.schematic_conversion.structure_to_litematica_failed");
            }

            return litematicaSchematic;
        }
        catch (Throwable t)
        {
        }

        return null;
    }

    private static Template readTemplateFromStream(InputStream stream, DataFixer fixer) throws IOException
    {
        NBTTagCompound nbt = CompressedStreamTools.readCompressed(stream);
        Template template = new Template();
        template.read(fixer.process(FixTypes.STRUCTURE, nbt));

        return template;
    }

    public static void loadChunksClientWorld(WorldClient world, BlockPos origin, Vec3i areaSize)
    {
        BlockPos posEnd = origin.add(PositionUtils.getRelativeEndPositionFromAreaSize(areaSize));
        BlockPos posMin = PositionUtils.getMinCorner(origin, posEnd);
        BlockPos posMax = PositionUtils.getMaxCorner(origin, posEnd);
        final int cxMin = posMin.getX() >> 4;
        final int czMin = posMin.getZ() >> 4;
        final int cxMax = posMax.getX() >> 4;
        final int czMax = posMax.getZ() >> 4;

        for (int cz = czMin; cz <= czMax; ++cz)
        {
            for (int cx = cxMin; cx <= cxMax; ++cx)
            {
                world.getChunkProvider().loadChunk(cx, cz);
            }
        }
    }

    /**
     * Best name. Returns the integrated server world for the current dimension
     * in single player, otherwise just the client world.
     * @param mc
     * @return
     */
    @Nullable
    public static World getBestWorld(Minecraft mc)
    {
        if (mc.isSingleplayer())
        {
            IntegratedServer server = mc.getIntegratedServer();
            return server.getWorld(mc.world.provider.getDimensionType().getId());
        }
        else
        {
            return mc.world;
        }
    }

    public static void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getChunkMapping();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getChunkMapping();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getRenderGlobal();
                rg.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
        }
    }

    public static void markSchematicChunksForRenderUpdateBetweenX(int x1, int x2)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int xMin = Math.min(x1, x2);
            final int xMax = Math.max(x1, x2);
            final int cxMin = (xMin >> 4);
            final int cxMax = (xMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getRenderGlobal();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getChunkMapping();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getChunkMapping();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.x >= cxMin && chunk.x <= cxMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    x1 = Math.max( chunk.x << 4      , xMin);
                    x2 = Math.min((chunk.x << 4) + 15, xMax);
                    rg.markBlockRangeForRenderUpdate(x1, 0, (chunk.z << 4), x2, 255, (chunk.z << 4) + 15);
                }
            }
        }
    }

    public static void markSchematicChunksForRenderUpdateBetweenY(int y1, int y2)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            RenderGlobal rg = LitematicaRenderer.getInstance().getRenderGlobal();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getChunkMapping();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getChunkMapping();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.isEmpty() == false && clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    rg.markBlockRangeForRenderUpdate((chunk.x << 4) - 1, y1, (chunk.z << 4) - 1, (chunk.x << 4) + 16, y2, (chunk.z << 4) + 16);
                }
            }
        }
    }

    public static void markSchematicChunksForRenderUpdateBetweenZ(int z1, int z2)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            final int zMin = Math.min(z1, z2);
            final int zMax = Math.max(z1, z2);
            final int czMin = (zMin >> 4);
            final int czMax = (zMax >> 4);
            RenderGlobal rg = LitematicaRenderer.getInstance().getRenderGlobal();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getChunkMapping();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getMinecraft().world.getChunkProvider()).getChunkMapping();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.z >= czMin && chunk.z <= czMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    z1 = Math.max( chunk.z << 4      , zMin);
                    z2 = Math.min((chunk.z << 4) + 15, zMax);
                    rg.markBlockRangeForRenderUpdate((chunk.x << 4), 0, z1, (chunk.x << 4) + 15, 255, z2);
                }
            }
        }
    }

    public static boolean doSchematicWorldPickBlock(boolean closest, Minecraft mc)
    {
        if (Configs.Generic.PICK_BLOCK_ENABLED.getBooleanValue() == false)
        {
            return false;
        }

        BlockPos pos = null;

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, mc.player, 6);
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldTrace(mc.world, mc.player, 6);
        }

        if (pos != null)
        {
            World world = SchematicWorldHandler.getSchematicWorld();
            IBlockState state = world.getBlockState(pos);
            ItemStack stack = ItemUtils.getItemForBlock(world, pos, state, true);

            // Don't pick-block and cancel further processing if the correct item is already in the player's hand
            if (stack.isEmpty() == false)
            {
                return InventoryUtils.swapItemToMainHand(stack, mc);
            }
        }

        return false;
    }

    public static void deleteSelectionVolumes(Minecraft mc)
    {
        if (mc.player != null && mc.player.capabilities.isCreativeMode)
        {
            final AreaSelection area = DataManager.getInstance().getSelectionManager().getCurrentSelection();

            if (area != null)
            {
                if (mc.isSingleplayer())
                {
                    final WorldServer world = mc.getIntegratedServer().getWorld(mc.player.getEntityWorld().provider.getDimensionType().getId());

                    world.addScheduledTask(new Runnable()
                    {
                        public void run()
                        {
                            Box currentBox = area.getSelectedSubRegionBox();
                            Collection<Box> boxes;

                            if (currentBox != null)
                            {
                                boxes = ImmutableList.of(currentBox);
                            }
                            else
                            {
                                boxes = area.getAllSubRegionBoxes();
                            }

                            if (deleteSelectionVolumes(world, boxes))
                            {
                                StringUtils.printActionbarMessage("litematica.message.area_cleared");
                            }
                            else
                            {
                                StringUtils.printActionbarMessage("litematica.message.area_clear_fail");
                            }
                        }
                    });

                    StringUtils.printActionbarMessage("litematica.message.scheduled_task_added");
                }
                else
                {
                    StringUtils.printActionbarMessage("litematica.message.only_works_in_single_player");
                }
            }
            else
            {
                StringUtils.printActionbarMessage("litematica.message.no_area_selected");
            }
        }
    }

    public static boolean deleteSelectionVolumes(World world, Collection<Box> boxes)
    {
        IBlockState air = Blocks.AIR.getDefaultState();
        IBlockState barrier = Blocks.BARRIER.getDefaultState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (Box box : boxes)
        {
            BlockPos posMin = PositionUtils.getMinCorner(box.getPos1(), box.getPos2());
            BlockPos posMax = PositionUtils.getMaxCorner(box.getPos1(), box.getPos2());

            for (int z = posMin.getZ(); z <= posMax.getZ(); ++z)
            {
                for (int x = posMin.getX(); x <= posMax.getX(); ++x)
                {
                    for (int y = posMax.getY(); y >= posMin.getY(); --y)
                    {
                        posMutable.setPos(x, y, z);
                        TileEntity te = world.getTileEntity(posMutable);

                        if (te instanceof IInventory)
                        {
                            ((IInventory) te).clear();
                            world.setBlockState(posMutable, barrier, 0x12);
                        }

                        world.setBlockState(posMutable, air, 0x12);
                    }
                }
            }
        }

        return true;
    }
}
