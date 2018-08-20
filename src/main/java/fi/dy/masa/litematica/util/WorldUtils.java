package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.interfaces.IMixinChunkProviderClient;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
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
        area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
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
            area.createNewSubRegionBox(BlockPos.ORIGIN, subRegionName);
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

            if (schematicChunks.containsKey(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4)))
            {
                RenderGlobal rg = LitematicaRenderer.getInstance().getRenderGlobal();
                rg.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            }
        }
    }

    public static void markSchematicChunksForRenderUpdateBetween(int y1, int y2)
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
}
