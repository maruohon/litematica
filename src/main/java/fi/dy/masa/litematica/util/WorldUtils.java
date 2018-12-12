package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.state.properties.ComparatorMode;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.SlabType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

public class WorldUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();
    private static boolean preventOnBlockAdded;

    public static boolean shouldPreventOnBlockAdded()
    {
        return preventOnBlockAdded;
    }

    public static void setShouldPreventOnBlockAdded(boolean prevent)
    {
        preventOnBlockAdded = prevent;
    }

    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static LitematicaSchematic convertSchematicaSchematicToLitematicaSchematic(File inputDir, String inputFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(new File(inputDir, inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            return null;
        }

        WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
        WorldSchematic world = new WorldSchematic(null, settings, DimensionType.NETHER, EnumDifficulty.NORMAL, Minecraft.getInstance().profiler);

        WorldUtils.loadChunksSchematicWorld(world, BlockPos.ORIGIN, schematic.getSize());
        PlacementSettings placementSettings = new PlacementSettings();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        area.getSelectedSubRegionBox().setPos1(BlockPos.ORIGIN); // createNewSubRegionBox() offsets the default position by one when creating the first box...
        area.getSelectedSubRegionBox().setPos2((new BlockPos(schematic.getSize())).add(-1, -1, -1));

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, false, "?", feedback);

        if (litematicaSchematic != null && ignoreEntities == false)
        {
            litematicaSchematic.takeEntityDataFromSchematicaSchematic(schematic, subRegionName);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
        }

        return litematicaSchematic;
    }

    public static boolean convertStructureToLitematicaSchematic(File structureDir, String structureFileName,
            File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(File structureDir, String structureFileName,
            boolean ignoreEntities, IStringConsumer feedback)
    {
        DataFixer fixer = Minecraft.getInstance().getDataFixer();
        File file = new File(structureDir, structureFileName);

        try
        {
            InputStream is = new FileInputStream(file);
            Template template = readTemplateFromStream(is, fixer);
            is.close();

            WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
            WorldSchematic world = new WorldSchematic(null, settings, DimensionType.NETHER, EnumDifficulty.NORMAL, Minecraft.getInstance().profiler);

            loadChunksSchematicWorld(world, BlockPos.ORIGIN, template.getSize());

            PlacementSettings placementSettings = new PlacementSettings();
            placementSettings.setIgnoreEntities(ignoreEntities);
            template.addBlocksToWorld(world, BlockPos.ORIGIN, null, placementSettings, 0x12);

            String subRegionName = FileUtils.getNameWithoutExtension(structureFileName) + " (Converted Structure)";
            AreaSelection area = new AreaSelection();
            area.setName(subRegionName);
            subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
            area.setSelectedSubRegionBox(subRegionName);
            area.getSelectedSubRegionBox().setPos1(BlockPos.ORIGIN); // createNewSubRegionBox() offsets the default position by one when creating the first box...
            area.getSelectedSubRegionBox().setPos2(template.getSize().add(-1, -1, -1));

            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, ignoreEntities, template.getAuthor(), feedback);

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
        // FIXME 1.13
        //template.read(fixer.process(FixTypes.STRUCTURE, nbt));
        template.read(nbt);

        return template;
    }

    public static void loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vec3i areaSize)
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
            return server.getWorld(mc.world.dimension.getType());
        }
        else
        {
            return mc.world;
        }
    }

    public static void markSchematicChunkForRenderUpdate(SubChunkPos chunkPos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getInstance().world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(chunkPos.getX(), chunkPos.getZ());

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
                renderer.markBlockRangeForRenderUpdate((chunkPos.getX() << 4) - 1, (chunkPos.getY() << 4) - 1, (chunkPos.getZ() << 4) - 1,
                                                       (chunkPos.getX() << 4) + 1, (chunkPos.getY() << 4) + 1, (chunkPos.getZ() << 4) + 1);
            }
        }
    }

    public static void markSchematicChunkForRenderUpdate(BlockPos pos)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getInstance().world.getChunkProvider()).getLoadedChunks();
            long key = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

            if (schematicChunks.containsKey(key) && clientChunks.containsKey(key))
            {
                WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
                renderer.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
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
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getInstance().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.x >= cxMin && chunk.x <= cxMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    x1 = Math.max( chunk.x << 4      , xMin);
                    x2 = Math.min((chunk.x << 4) + 15, xMax);
                    renderer.markBlockRangeForRenderUpdate(x1, 0, (chunk.z << 4), x2, 255, (chunk.z << 4) + 15);
                }
            }
        }
    }

    public static void markAllSchematicChunksForRenderUpdate()
    {
        markSchematicChunksForRenderUpdateBetweenY(LayerRange.WORLD_VERTICAL_SIZE_MIN, LayerRange.WORLD_VERTICAL_SIZE_MAX);
    }

    public static void markSchematicChunksForRenderUpdateBetweenY(int y1, int y2)
    {
        World world = SchematicWorldHandler.getSchematicWorld();

        if (world != null)
        {
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getInstance().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.isEmpty() == false && clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    renderer.markBlockRangeForRenderUpdate((chunk.x << 4) - 1, y1, (chunk.z << 4) - 1, (chunk.x << 4) + 16, y2, (chunk.z << 4) + 16);
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
            WorldRenderer renderer = LitematicaRenderer.getInstance().getWorldRenderer();
            Long2ObjectMap<Chunk> schematicChunks = ((IMixinChunkProviderClient) (Object) world.getChunkProvider()).getLoadedChunks();
            Long2ObjectMap<Chunk> clientChunks = ((IMixinChunkProviderClient) (Object) Minecraft.getInstance().world.getChunkProvider()).getLoadedChunks();

            for (Chunk chunk : schematicChunks.values())
            {
                // Only mark chunks that are actually rendered (if the schematic world contains more chunks)
                if (chunk.z >= czMin && chunk.z <= czMax && chunk.isEmpty() == false &&
                    clientChunks.containsKey(ChunkPos.asLong(chunk.x, chunk.z)))
                {
                    z1 = Math.max( chunk.z << 4      , zMin);
                    z2 = Math.min((chunk.z << 4) + 15, zMax);
                    renderer.markBlockRangeForRenderUpdate((chunk.x << 4), 0, z1, (chunk.x << 4) + 15, 255, z2);
                }
            }
        }
    }

    /**
     * Does a ray trace to the schematic world, and returns either the closest or the furthest hit block.
     * @param closest
     * @param mc
     * @return true if the correct item was or is in the player's hand after the pick block
     */
    public static boolean doSchematicWorldPickBlock(boolean closest, Minecraft mc)
    {
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

            if (stack.isEmpty() == false)
            {
                InventoryPlayer inv = mc.player.inventory;

                if (mc.player.abilities.isCreativeMode)
                {
                    TileEntity te = world.getTileEntity(pos);

                    // The creative mode pick block with NBT only works correctly
                    // if the server world doesn't have a TileEntity in that position.
                    // Otherwise it would try to write whatever that TE is into the picked ItemStack.
                    if (GuiScreen.isCtrlKeyDown() && te != null && mc.world.isAirBlock(pos))
                    {
                        ItemUtils.storeTEInStack(stack, te);
                    }

                    InventoryUtils.setPickedItemToHand(stack, mc);
                    mc.playerController.sendSlotPacket(mc.player.getHeldItem(EnumHand.MAIN_HAND), 36 + inv.currentItem);

                    return true;
                }
                else
                {
                    int slot = inv.getSlotFor(stack);
                    boolean shouldPick = inv.currentItem != slot;
                    boolean canPick = slot != -1;

                    if (shouldPick && canPick)
                    {
                        InventoryUtils.setPickedItemToHand(stack, mc);
                    }

                    return shouldPick == false || canPick;
                }
            }
        }

        return false;
    }

    public static void easyPlaceOnUseTick(Minecraft mc)
    {
        if (Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            mc.player != null &&
            (Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld() ||
             Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isValid() == false) &&
             mc.gameSettings.keyBindUseItem.isKeyDown())
        {
            WorldUtils.handleEasyPlace(mc);
        }
    }

    public static boolean handleEasyPlace(Minecraft mc)
    {
        RayTraceWrapper traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true);

        if (traceWrapper != null && traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            RayTraceResult trace = traceWrapper.getRayTraceResult();
            BlockPos pos = trace.getBlockPos();
            World world = SchematicWorldHandler.getSchematicWorld();
            IBlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = ItemUtils.getItemForBlock(world, pos, stateSchematic, true);

            // Already placed to that position, possible server sync delay
            if (easyPlaceIsPositionCached(pos))
            {
                return true;
            }

            if (stack.isEmpty() == false)
            {
                EnumHand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // Abort if the wrong item is in the player's hand
                if (hand == null)
                {
                    return true;
                }

                stack = mc.player.getHeldItem(hand);
                IBlockState stateClient = mc.world.getBlockState(pos);

                if (stateSchematic == stateClient)
                {
                    return true;
                }

                // Abort if there is already a block in the target position
                if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, trace, stack))
                {
                    return true;
                }

                // Abort if the required item was not able to be pick-block'd
                if (doSchematicWorldPickBlock(true, mc) == false)
                {
                    return true;
                }

                Vec3d hitPos = trace.hitVec;
                EnumFacing sideOrig = trace.sideHit;
                EnumFacing side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Carpet Accurate Placement protocol support, plus BlockSlab support
                hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos);

                //System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hitPos, hand);

                if (stateSchematic.getBlock() instanceof BlockSlab && stateSchematic.get(BlockSlab.TYPE) == SlabType.DOUBLE)
                {
                    stateClient = mc.world.getBlockState(pos);

                    if (stateClient.getBlock() instanceof BlockSlab && stateClient.get(BlockSlab.TYPE) != SlabType.DOUBLE)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        mc.playerController.processRightClickBlock(mc.player, mc.world, pos, side, hitPos, hand);
                    }
                }
            }
        }

        return true;
    }

    private static boolean easyPlaceBlockChecksCancel(IBlockState stateSchematic, IBlockState stateClient,
            EntityPlayer player, RayTraceResult trace, ItemStack stack)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof BlockSlab && stateSchematic.get(BlockSlab.TYPE) == SlabType.DOUBLE)
        {
            Block blockClient = stateSchematic.getBlock();

            if (blockClient instanceof BlockSlab && stateClient.get(BlockSlab.TYPE) != SlabType.DOUBLE)
            {
                return blockSchematic != blockClient;
            }
        }

        Vec3d hit = trace.hitVec;
        BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(player, stack, trace.getBlockPos(), trace.sideHit, (float) hit.x, (float) hit.y, (float) hit.z));

        if (stateClient.isReplaceable(ctx) == false)
        {
            return true;
        }

        return false;
    }

    public static Vec3d applyCarpetProtocolHitVec(BlockPos pos, IBlockState state, Vec3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        EnumFacing facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);

        if (facing != null)
        {
            x = facing.ordinal() + 2 + pos.getX();
        }

        if (block instanceof BlockRedstoneRepeater)
        {
            x += ((state.get(BlockRedstoneRepeater.DELAY)) - 1) * 10;
        }
        else if (block instanceof BlockTrapDoor && state.get(BlockTrapDoor.HALF) == Half.TOP)
        {
            x += 10;
        }
        else if (block instanceof BlockRedstoneComparator && state.get(BlockRedstoneComparator.MODE) == ComparatorMode.SUBTRACT)
        {
            x += 10;
        }
        else if (block instanceof BlockStairs && state.get(BlockStairs.HALF) == Half.TOP)
        {
            x += 10;
        }
        else if (block instanceof BlockSlab && state.get(BlockSlab.TYPE) != SlabType.DOUBLE)
        {
            //x += 10; // Doesn't actually exist (yet?)

            // Do it via vanilla
            if (state.get(BlockSlab.TYPE) == SlabType.TOP)
            {
                y = pos.getY() + 0.9;
            }
            else
            {
                y = pos.getY();
            }
        }

        return new Vec3d(x, y, z);
    }

    private static EnumFacing applyPlacementFacing(IBlockState stateSchematic, EnumFacing side, IBlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof BlockSlab)
        {
            if (stateSchematic.get(BlockSlab.TYPE) == SlabType.DOUBLE &&
                blockClient instanceof BlockSlab &&
                stateClient.get(BlockSlab.TYPE) != SlabType.DOUBLE)
            {
                if (stateClient.get(BlockSlab.TYPE) == SlabType.TOP)
                {
                    return EnumFacing.DOWN;
                }
                else
                {
                    return EnumFacing.UP;
                }
            }
            // Single slab
            else
            {
                return EnumFacing.NORTH;
            }
        }

        return side;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @param doEasyPlace
     * @param restrictPlacement
     * @return
     */
    public static boolean handlePlacementRestriction(Minecraft mc)
    {
        RayTraceResult trace = mc.objectMouseOver;

        if (trace.type == RayTraceResult.Type.BLOCK)
        {
            BlockPos pos = trace.getBlockPos();
            IBlockState stateClient = mc.world.getBlockState(pos);
            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();

            // There should not be anything in the targeted position
            if (range.isPositionWithinRange(pos) == false || worldSchematic.isAirBlock(pos))
            {
                return true;
            }

            IBlockState stateSchematic = worldSchematic.getBlockState(pos);
            ItemStack stack = ItemUtils.getItemForBlock(worldSchematic, pos, stateSchematic, true);

            if (stack.isEmpty() == false)
            {
                EnumHand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // The player is holding the wrong item for the targeted position
                if (hand == null)
                {
                    return true;
                }

                Vec3d hit = trace.hitVec;
                BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(mc.player, stack, pos, trace.sideHit, (float) hit.x, (float) hit.y, (float) hit.z));

                // Placement position is not already occupied
                return stateClient.isReplaceable(ctx);
            }

            return true;
        }

        return false;
    }

    public static void deleteSelectionVolumes(Minecraft mc)
    {
        if (mc.player != null && mc.player.abilities.isCreativeMode)
        {
            final AreaSelection area = DataManager.getSelectionManager().getCurrentSelection();

            if (area != null)
            {
                if (mc.isSingleplayer())
                {
                    final WorldServer world = mc.getIntegratedServer().getWorld(mc.player.getEntityWorld().dimension.getType());

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

    public static boolean easyPlaceIsPositionCached(BlockPos pos)
    {
        long currentTime = System.nanoTime();
        boolean cached = false;

        for (int i = 0; i < EASY_PLACE_POSITIONS.size(); ++i)
        {
            PositionCache val = EASY_PLACE_POSITIONS.get(i);
            boolean expired = val.hasExpired(currentTime);

            if (expired)
            {
                EASY_PLACE_POSITIONS.remove(i);
                --i;
            }
            else if (val.getPos().equals(pos))
            {
                cached = true;

                // Keep checking and removing old entries if there are a fair amount
                if (EASY_PLACE_POSITIONS.size() < 16)
                {
                    break;
                }
            }
        }

        return cached;
    }

    private static void cacheEasyPlacePosition(BlockPos pos)
    {
        EASY_PLACE_POSITIONS.add(new PositionCache(pos, System.nanoTime(), 2000000000));
    }

    public static class PositionCache
    {
        private final BlockPos pos;
        private final long time;
        private final long timeout;

        private PositionCache(BlockPos pos, long time, long timeout)
        {
            this.pos = pos;
            this.time = time;
            this.timeout = timeout;
        }

        public BlockPos getPos()
        {
            return this.pos;
        }

        public boolean hasExpired(long currentTime)
        {
            return currentTime - this.time > this.timeout;
        }
    }
}
