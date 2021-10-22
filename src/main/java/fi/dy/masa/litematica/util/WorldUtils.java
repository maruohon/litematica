package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import com.mojang.datafixers.DataFixer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapDoorBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.Property;
import net.minecraft.state.properties.ComparatorMode;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.ActionResultType;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.MaterialCache;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.util.PositionUtils.Corner;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.interfaces.IStringConsumer;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.SubChunkPos;

public class WorldUtils
{
    private static final List<PositionCache> EASY_PLACE_POSITIONS = new ArrayList<>();

    public static boolean shouldPreventBlockUpdates(World world)
    {
        return ((IWorldUpdateSuppressor) world).litematica_getShouldPreventBlockUpdates();
    }

    public static void setShouldPreventBlockUpdates(World world, boolean preventUpdates)
    {
        ((IWorldUpdateSuppressor) world).litematica_setShouldPreventBlockUpdates(preventUpdates);
    }

    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = convertSchematicaSchematicToLitematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
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

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        loadChunksSchematicWorld(world, BlockPos.ORIGIN, schematic.getSize());
        PlacementSettings placementSettings = new PlacementSettings();
        placementSettings.setIgnoreEntities(ignoreEntities);
        schematic.placeSchematicDirectlyToChunks(world, BlockPos.ORIGIN, placementSettings);

        String subRegionName = FileUtils.getNameWithoutExtension(inputFileName) + " (Converted Schematic)";
        AreaSelection area = new AreaSelection();
        area.setName(subRegionName);
        subRegionName = area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
        area.setSelectedSubRegionBox(subRegionName);
        Box box = area.getSelectedSubRegionBox();
        area.setSubRegionCornerPos(box, Corner.CORNER_1, BlockPos.ORIGIN);
        area.setSubRegionCornerPos(box, Corner.CORNER_2, (new BlockPos(schematic.getSize())).add(-1, -1, -1));
        LitematicaSchematic.SchematicSaveInfo info = new LitematicaSchematic.SchematicSaveInfo(false, false);

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, info, "?", feedback);

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
            File outputDir, String outputFileName, boolean override)
    {
        LitematicaSchematic litematicaSchematic = convertStructureToLitematicaSchematic(structureDir, structureFileName);
        return litematicaSchematic != null && litematicaSchematic.writeToFile(outputDir, outputFileName, override);
    }

    @Nullable
    public static LitematicaSchematic convertSpongeSchematicToLitematicaSchematic(File dir, String fileName)
    {
        try
        {
            LitematicaSchematic schematic = LitematicaSchematic.createFromFile(dir, fileName, FileType.SPONGE_SCHEMATIC);

            if (schematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the Sponge schematic from '" + fileName + '"');
            }

            return schematic;
        }
        catch (Exception e)
        {
            String msg = "Exception while trying to load the Sponge schematic: " + e.getMessage();
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, msg);
            Litematica.logger.error(msg);
        }

        return null;
    }

    @Nullable
    public static LitematicaSchematic convertStructureToLitematicaSchematic(File structureDir, String structureFileName)
    {
        try
        {
            LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(structureDir, structureFileName, FileType.VANILLA_STRUCTURE);

            if (litematicaSchematic == null)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Failed to read the vanilla structure template from '" + structureFileName + '"');
            }

            return litematicaSchematic;
        }
        catch (Exception e)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "Exception while trying to load the vanilla structure: " + e.getMessage());
            Litematica.logger.error("Exception while trying to load the vanilla structure: " + e.getMessage());
        }

        return null;
    }

    public static boolean convertLitematicaSchematicToSchematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        //SchematicaSchematic schematic = convertLitematicaSchematicToSchematicaSchematic(inputDir, inputFileName, ignoreEntities, feedback);
        //return schematic != null && schematic.writeToFile(outputDir, outputFileName, override, feedback);
        // TODO 1.13
        return false;
    }

    public static boolean convertLitematicaSchematicToVanillaStructure(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean ignoreEntities, boolean override, IStringConsumer feedback)
    {
        Template template = convertLitematicaSchematicToVanillaStructure(inputDir, inputFileName, ignoreEntities, feedback);
        return writeVanillaStructureToFile(template, outputDir, outputFileName, override, feedback);
    }

    @Nullable
    public static Template convertLitematicaSchematicToVanillaStructure(File inputDir, String inputFileName, boolean ignoreEntities, IStringConsumer feedback)
    {
        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromFile(inputDir, inputFileName);

        if (litematicaSchematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.litematica_to_schematic.failed_to_read_schematic");
            return null;
        }

        WorldSchematic world = SchematicWorldHandler.createSchematicWorld();

        BlockPos size = new BlockPos(litematicaSchematic.getTotalSize());
        loadChunksSchematicWorld(world, BlockPos.ORIGIN, size);
        SchematicPlacement schematicPlacement = SchematicPlacement.createForSchematicConversion(litematicaSchematic, BlockPos.ORIGIN);
        litematicaSchematic.placeToWorld(world, schematicPlacement, false); // TODO use a per-chunk version for a bit more speed

        Template template = new Template();
        template.saveFromWorld(world, BlockPos.ORIGIN, size, ignoreEntities == false, Blocks.STRUCTURE_VOID);

        return template;
    }

    private static boolean writeVanillaStructureToFile(Template template, File dir, String fileNameIn, boolean override, IStringConsumer feedback)
    {
        String fileName = fileNameIn;
        String extension = ".nbt";

        if (fileName.endsWith(extension) == false)
        {
            fileName = fileName + extension;
        }

        File file = new File(dir, fileName);
        FileOutputStream os = null;

        try
        {
            if (dir.exists() == false && dir.mkdirs() == false)
            {
                feedback.setString(StringUtils.translate("litematica.error.schematic_write_to_file_failed.directory_creation_failed", dir.getAbsolutePath()));
                return false;
            }

            if (override == false && file.exists())
            {
                feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exists", file.getAbsolutePath()));
                return false;
            }

            CompoundNBT tag = template.toTag(new CompoundNBT());
            os = new FileOutputStream(file);
            CompressedStreamTools.writeCompressed(tag, os);
            os.close();

            return true;
        }
        catch (Exception e)
        {
            feedback.setString(StringUtils.translate("litematica.error.structure_write_to_file_failed.exception", file.getAbsolutePath()));
        }

        return false;
    }

    private static Template readTemplateFromStream(InputStream stream, DataFixer fixer) throws IOException
    {
        CompoundNBT nbt = CompressedStreamTools.readCompressed(stream);
        Template template = new Template();
        //template.read(fixer.process(FixTypes.STRUCTURE, nbt));
        template.fromTag(nbt);

        return template;
    }

    public static boolean isClientChunkLoaded(ClientWorld world, int chunkX, int chunkZ)
    {
        return ((ClientChunkProvider) world.getChunkManager()).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    public static void loadChunksSchematicWorld(WorldSchematic world, BlockPos origin, Vector3i areaSize)
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

    public static void setToolModeBlockState(ToolMode mode, boolean primary, Minecraft mc)
    {
        BlockState state = Blocks.AIR.getDefaultState();
        Entity entity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(mc.world, entity, 6);

        if (wrapper != null)
        {
            BlockRayTraceResult trace = wrapper.getBlockHitResult();

            if (trace != null && trace.getType() == RayTraceResult.Type.BLOCK)
            {
                BlockPos pos = trace.getBlockPos();

                if (wrapper.getHitType() == HitType.SCHEMATIC_BLOCK)
                {
                    state = SchematicWorldHandler.getSchematicWorld().getBlockState(pos);
                }
                else if (wrapper.getHitType() == HitType.VANILLA_BLOCK)
                {
                    state = mc.world.getBlockState(pos);
                }
            }
        }

        if (primary)
        {
            mode.setPrimaryBlock(state);
        }
        else
        {
            mode.setSecondaryBlock(state);
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
        BlockPos pos;

        if (closest)
        {
            pos = RayTraceUtils.getSchematicWorldTraceIfClosest(mc.world, mc.player, 6);
        }
        else
        {
            pos = RayTraceUtils.getFurthestSchematicWorldBlockBeforeVanilla(mc.world, mc.player, 6, true);
        }

        if (pos != null)
        {
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState state = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(state, world, pos);

            InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);

            return true;
        }

        return false;
    }

    public static void easyPlaceOnUseTick(Minecraft mc)
    {
        if (mc.player != null && DataManager.getToolMode() != ToolMode.REBUILD &&
            Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            Configs.Generic.EASY_PLACE_HOLD_ENABLED.getBooleanValue() &&
            Hotkeys.EASY_PLACE_ACTIVATION.getKeybind().isKeybindHeld())
        {
            WorldUtils.doEasyPlaceAction(mc);
        }
    }

    public static boolean handleEasyPlace(Minecraft mc)
    {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() &&
            DataManager.getToolMode() != ToolMode.REBUILD)
        {
            ActionResultType result = doEasyPlaceAction(mc);

            if (result == ActionResultType.FAIL)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.easy_place_fail");
                return true;
            }

            return result != ActionResultType.PASS;
        }

        return false;
    }

    private static ActionResultType doEasyPlaceAction(Minecraft mc)
    {
        RayTraceWrapper traceWrapper;

        if (Configs.Generic.EASY_PLACE_FIRST.getBooleanValue())
        {
            // Temporary hack, using this same config here
            boolean targetFluids = Configs.InfoOverlays.INFO_OVERLAYS_TARGET_FLUIDS.getBooleanValue();
            traceWrapper = RayTraceUtils.getGenericTrace(mc.world, mc.player, 6, true, targetFluids, false);
        }
        else
        {
            traceWrapper = RayTraceUtils.getFurthestSchematicWorldTraceBeforeVanilla(mc.world, mc.player, 6);
        }

        if (traceWrapper == null)
        {
            return ActionResultType.PASS;
        }

        if (traceWrapper.getHitType() == RayTraceWrapper.HitType.SCHEMATIC_BLOCK)
        {
            BlockRayTraceResult trace = traceWrapper.getBlockHitResult();
            RayTraceResult traceVanilla = RayTraceUtils.getRayTraceFromEntity(mc.world, mc.player, false, 6);
            BlockPos pos = trace.getBlockPos();
            World world = SchematicWorldHandler.getSchematicWorld();
            BlockState stateSchematic = world.getBlockState(pos);
            ItemStack stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // Already placed to that position, possible server sync delay
            if (easyPlaceIsPositionCached(pos))
            {
                return ActionResultType.FAIL;
            }

            if (stack.isEmpty() == false)
            {
                BlockState stateClient = mc.world.getBlockState(pos);

                if (stateSchematic == stateClient)
                {
                    return ActionResultType.FAIL;
                }

                // Abort if there is already a block in the target position
                if (easyPlaceBlockChecksCancel(stateSchematic, stateClient, mc.player, traceVanilla, stack))
                {
                    return ActionResultType.FAIL;
                }

                InventoryUtils.schematicWorldPickBlock(stack, pos, world, mc);
                Hand hand = EntityUtils.getUsedHandForItem(mc.player, stack);

                // Abort if a wrong item is in the player's hand
                if (hand == null)
                {
                    return ActionResultType.FAIL;
                }

                Vector3d hitPos = trace.getPos();
                Direction sideOrig = trace.getSide();

                // If there is a block in the world right behind the targeted schematic block, then use
                // that block as the click position
                if (traceVanilla != null && traceVanilla.getType() == RayTraceResult.Type.BLOCK)
                {
                    BlockRayTraceResult hitResult = (BlockRayTraceResult) traceVanilla;
                    BlockPos posVanilla = hitResult.getBlockPos();
                    Direction sideVanilla = hitResult.getSide();
                    BlockState stateVanilla = mc.world.getBlockState(posVanilla);
                    Vector3d hit = traceVanilla.getPos();
                    BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(mc.player, hand, hitResult));

                    if (stateVanilla.canReplace(ctx) == false)
                    {
                        posVanilla = posVanilla.offset(sideVanilla);

                        if (pos.equals(posVanilla))
                        {
                            hitPos = hit;
                            sideOrig = sideVanilla;
                        }
                    }
                }

                Direction side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);

                // Carpet Accurate Placement protocol support, plus BlockSlab support
                if (Configs.Generic.EASY_PLACE_PROTOCOL_V3.getBooleanValue())
                {
                    hitPos = applyPlacementProtocolV3(pos, stateSchematic, hitPos);
                }
                else
                {
                    hitPos = applyCarpetProtocolHitVec(pos, stateSchematic, hitPos);
                }

                // Mark that this position has been handled (use the non-offset position that is checked above)
                cacheEasyPlacePosition(pos);

                BlockRayTraceResult hitResult = new BlockRayTraceResult(hitPos, side, pos, false);

                //System.out.printf("pos: %s side: %s, hit: %s\n", pos, side, hitPos);
                // pos, side, hitPos
                mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);

                if (stateSchematic.getBlock() instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
                {
                    stateClient = mc.world.getBlockState(pos);

                    if (stateClient.getBlock() instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
                    {
                        side = applyPlacementFacing(stateSchematic, sideOrig, stateClient);
                        hitResult = new BlockRayTraceResult(hitPos, side, pos, false);
                        mc.interactionManager.interactBlock(mc.player, mc.world, hand, hitResult);
                    }
                }
            }

            return ActionResultType.SUCCESS;
        }
        else if (traceWrapper.getHitType() == RayTraceWrapper.HitType.VANILLA_BLOCK)
        {
            return placementRestrictionInEffect(mc) ? ActionResultType.FAIL : ActionResultType.PASS;
        }

        return ActionResultType.PASS;
    }

    private static boolean easyPlaceBlockChecksCancel(BlockState stateSchematic, BlockState stateClient,
            PlayerEntity player, RayTraceResult trace, ItemStack stack)
    {
        Block blockSchematic = stateSchematic.getBlock();

        if (blockSchematic instanceof SlabBlock && stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE)
        {
            Block blockClient = stateClient.getBlock();

            if (blockClient instanceof SlabBlock && stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                return blockSchematic != blockClient;
            }
        }

        if (trace.getType() != RayTraceResult.Type.BLOCK)
        {
            return false;
        }

        BlockRayTraceResult hitResult = (BlockRayTraceResult) trace;
        BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(player, Hand.MAIN_HAND, hitResult));

        if (stateClient.canReplace(ctx) == false)
        {
            return true;
        }

        return false;
    }

    /**
     * Apply the Carpet-Extra mod accurate block placement protocol support
     */
    public static Vector3d applyCarpetProtocolHitVec(BlockPos pos, BlockState state, Vector3d hitVecIn)
    {
        double x = hitVecIn.x;
        double y = hitVecIn.y;
        double z = hitVecIn.z;
        Block block = state.getBlock();
        Direction facing = fi.dy.masa.malilib.util.BlockUtils.getFirstPropertyFacingValue(state);
        final int propertyIncrement = 16;
        double relX = hitVecIn.x - pos.getX();

        if (facing != null)
        {
            x = pos.getX() + relX + 2 + (facing.getId() * 2);
        }

        if (block instanceof RepeaterBlock)
        {
            x += ((state.get(RepeaterBlock.DELAY)) - 1) * propertyIncrement;
        }
        else if (block instanceof TrapDoorBlock && state.get(TrapDoorBlock.HALF) == Half.TOP)
        {
            x += propertyIncrement;
        }
        else if (block instanceof ComparatorBlock && state.get(ComparatorBlock.MODE) == ComparatorMode.SUBTRACT)
        {
            x += propertyIncrement;
        }
        else if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == Half.TOP)
        {
            x += propertyIncrement;
        }
        else if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) != SlabType.DOUBLE)
        {
            //x += 10; // Doesn't actually exist (yet?)

            // Do it via vanilla
            if (state.get(SlabBlock.TYPE) == SlabType.TOP)
            {
                y = pos.getY() + 0.9;
            }
            else
            {
                y = pos.getY();
            }
        }

        return new Vector3d(x, y, z);
    }

    public static <T extends Comparable<T>> Vector3d applyPlacementProtocolV3(BlockPos pos, BlockState state, Vector3d hitVecIn)
    {
        Collection<Property<?>> props = state.getBlock().getStateManager().getProperties();

        if (props.isEmpty())
        {
            return hitVecIn;
        }

        double relX = hitVecIn.x - pos.getX();
        int protocolValue = 0;
        int shiftAmount = 1;
        int propCount = 0;

        @Nullable DirectionProperty property = fi.dy.masa.malilib.util.BlockUtils.getFirstDirectionProperty(state);

        // DirectionProperty - allow all except: VERTICAL_DIRECTION (PointedDripstone)
        if (property != null)
        {
            Direction direction = state.get(property);
            protocolValue |= direction.getId() << shiftAmount;
            shiftAmount += 3;
            ++propCount;
        }

        List<Property<?>> propList = new ArrayList<>(props);
        propList.sort(Comparator.comparing(Property::getName));

        try
        {
            for (Property<?> p : propList)
            {
                if ((p instanceof DirectionProperty) == false &&
                    PlacementHandler.WHITELISTED_PROPERTIES.contains(p))
                {
                    @SuppressWarnings("unchecked")
                    Property<T> prop = (Property<T>) p;
                    List<T> list = new ArrayList<>(prop.getValues());
                    list.sort(Comparable::compareTo);

                    int requiredBits = MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(list.size()));
                    int valueIndex = list.indexOf(state.get(prop));

                    if (valueIndex != -1)
                    {
                        //System.out.printf("requesting: %s = %s, index: %d\n", prop.getName(), state.get(prop), valueIndex);
                        protocolValue |= (valueIndex << shiftAmount);
                        shiftAmount += requiredBits;
                        ++propCount;
                    }
                }
            }
        }
        catch (Exception e)
        {
            Litematica.logger.warn("Exception trying to request placement protocol value", e);
        }

        if (propCount > 0)
        {
            double x = pos.getX() + relX + 2 + protocolValue;
            //System.out.printf("request prot value 0x%08X\n", protocolValue + 2);
            return new Vector3d(x, hitVecIn.y, hitVecIn.z);
        }

        return hitVecIn;
    }

    private static Direction applyPlacementFacing(BlockState stateSchematic, Direction side, BlockState stateClient)
    {
        Block blockSchematic = stateSchematic.getBlock();
        Block blockClient = stateClient.getBlock();

        if (blockSchematic instanceof SlabBlock)
        {
            if (stateSchematic.get(SlabBlock.TYPE) == SlabType.DOUBLE &&
                blockClient instanceof SlabBlock &&
                stateClient.get(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                if (stateClient.get(SlabBlock.TYPE) == SlabType.TOP)
                {
                    return Direction.DOWN;
                }
                else
                {
                    return Direction.UP;
                }
            }
            // Single slab
            else
            {
                return Direction.NORTH;
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
     * @return
     */
    public static boolean handlePlacementRestriction(Minecraft mc)
    {
        boolean cancel = placementRestrictionInEffect(mc);

        if (cancel)
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.placement_restriction_fail");
        }

        return cancel;
    }

    /**
     * Does placement restriction checks for the targeted position.
     * If the targeted position is outside of the current layer range, or should be air
     * in the schematic, or the player is holding the wrong item in hand, then true is returned
     * to indicate that the use action should be cancelled.
     * @param mc
     * @return true if the use action should be cancelled
     */
    private static boolean placementRestrictionInEffect(Minecraft mc)
    {
        RayTraceResult trace = mc.crosshairTarget;

        ItemStack stack = mc.player.getMainHandStack();

        if (stack.isEmpty())
        {
            stack = mc.player.getOffHandStack();
        }

        if (stack.isEmpty())
        {
            return false;
        }

        if (trace != null && trace.getType() == RayTraceResult.Type.BLOCK)
        {
            BlockRayTraceResult blockHitResult = (BlockRayTraceResult) trace;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockItemUseContext ctx = new BlockItemUseContext(new ItemUseContext(mc.player, Hand.MAIN_HAND, blockHitResult));

            // Get the possibly offset position, if the targeted block is not replaceable
            pos = ctx.getBlockPos();

            BlockState stateClient = mc.world.getBlockState(pos);

            World worldSchematic = SchematicWorldHandler.getSchematicWorld();
            LayerRange range = DataManager.getRenderLayerRange();
            boolean schematicHasAir = worldSchematic.isAir(pos);

            // The targeted position is outside the current render range
            if (schematicHasAir == false && range.isPositionWithinRange(pos) == false)
            {
                return true;
            }

            // There should not be anything in the targeted position,
            // and the position is within or close to a schematic sub-region
            if (schematicHasAir && isPositionWithinRangeOfSchematicRegions(pos, 2))
            {
                return true;
            }

            blockHitResult = new BlockRayTraceResult(blockHitResult.getPos(), blockHitResult.getSide(), pos, false);
            ctx = new BlockItemUseContext(new ItemUseContext(mc.player, Hand.MAIN_HAND, (BlockRayTraceResult) trace));

            // Placement position is already occupied
            if (stateClient.canReplace(ctx) == false)
            {
                return true;
            }

            BlockState stateSchematic = worldSchematic.getBlockState(pos);
            stack = MaterialCache.getInstance().getRequiredBuildItemForState(stateSchematic);

            // The player is holding the wrong item for the targeted position
            if (stack.isEmpty() == false && EntityUtils.getUsedHandForItem(mc.player, stack) == null)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isPositionWithinRangeOfSchematicRegions(BlockPos pos, int range)
    {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        final int minCX = (pos.getX() - range) >> 4;
        final int minCY = (pos.getY() - range) >> 4;
        final int minCZ = (pos.getZ() - range) >> 4;
        final int maxCX = (pos.getX() + range) >> 4;
        final int maxCY = (pos.getY() + range) >> 4;
        final int maxCZ = (pos.getZ() + range) >> 4;
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int cy = minCY; cy <= maxCY; ++cy)
        {
            for (int cz = minCZ; cz <= maxCZ; ++cz)
            {
                for (int cx = minCX; cx <= maxCX; ++cx)
                {
                    List<IntBoundingBox> boxes = manager.getTouchedBoxesInSubChunk(new SubChunkPos(cx, cy, cz));

                    for (int i = 0; i < boxes.size(); ++i)
                    {
                        IntBoundingBox box = boxes.get(i);

                        if (x >= box.minX - range && x <= box.maxX + range &&
                            y >= box.minY - range && y <= box.maxY + range &&
                            z >= box.minZ - range && z <= box.maxZ + range)
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if the given one block thick slice has non-air blocks or not.
     * NOTE: The axis is the perpendicular axis (that goes through the plane).
     * @param axis
     * @param pos1
     * @param pos2
     * @return
     */
    public static boolean isSliceEmpty(World world, Direction.Axis axis, BlockPos pos1, BlockPos pos2)
    {
        BlockPos.Mutable posMutable = new BlockPos.Mutable();

        switch (axis)
        {
            case Z:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int z = pos1.getZ();
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);

                for (int cx = cxMin; cx <= cxMax; ++cx)
                {
                    Chunk chunk = world.getChunk(cx, z >> 4);
                    int xMin = Math.max(x1,  cx << 4      );
                    int xMax = Math.min(x2, (cx << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int x = xMin; x <= xMax; ++x)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
            }

            case Y:
            {
                int x1 = Math.min(pos1.getX(), pos2.getX());
                int x2 = Math.max(pos1.getX(), pos2.getX());
                int y = pos1.getY();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int cxMin = (x1 >> 4);
                int cxMax = (x2 >> 4);
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    for (int cx = cxMin; cx <= cxMax; ++cx)
                    {
                        Chunk chunk = world.getChunk(cx, cz);

                        if (y > chunk.getHighestNonEmptySectionYOffset() + 15)
                        {
                            continue;
                        }

                        int xMin = Math.max(x1,  cx << 4      );
                        int xMax = Math.min(x2, (cx << 4) + 15);
                        int zMin = Math.max(z1,  cz << 4      );
                        int zMax = Math.min(z2, (cz << 4) + 15);

                        for (int z = zMin; z <= zMax; ++z)
                        {
                            for (int x = xMin; x <= xMax; ++x)
                            {
                                if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                                {
                                    return false;
                                }
                            }
                        }
                    }
                }

                break;
            }

            case X:
            {
                int x = pos1.getX();
                int z1 = Math.min(pos1.getZ(), pos2.getZ());
                int z2 = Math.max(pos1.getZ(), pos2.getZ());
                int y1 = Math.min(pos1.getY(), pos2.getY());
                int y2 = Math.max(pos1.getY(), pos2.getY());
                int czMin = (z1 >> 4);
                int czMax = (z2 >> 4);

                for (int cz = czMin; cz <= czMax; ++cz)
                {
                    Chunk chunk = world.getChunk(x >> 4, cz);
                    int zMin = Math.max(z1,  cz << 4      );
                    int zMax = Math.min(z2, (cz << 4) + 15);
                    int yMax = Math.min(y2, chunk.getHighestNonEmptySectionYOffset() + 15);

                    for (int z = zMin; z <= zMax; ++z)
                    {
                        for (int y = y1; y <= yMax; ++y)
                        {
                            if (chunk.getBlockState(posMutable.set(x, y, z)).isAir() == false)
                            {
                                return false;
                            }
                        }
                    }
                }

                break;
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
