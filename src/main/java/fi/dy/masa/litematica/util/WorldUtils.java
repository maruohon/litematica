package fi.dy.masa.litematica.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import fi.dy.masa.litematica.interfaces.IStringConsumer;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.SchematicaSchematic;
import fi.dy.masa.litematica.selection.AreaSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

public class WorldUtils
{
    public static boolean convertSchematicaSchematicToLitematicaSchematic(
            File inputDir, String inputFileName, File outputDir, String outputFileName, boolean override, IStringConsumer feedback)
    {
        SchematicaSchematic schematic = SchematicaSchematic.createFromFile(new File(inputDir, inputFileName));

        if (schematic == null)
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_read_schematic");
            return false;
        }

        WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
        WorldClient world = new WorldClient(null, settings, 0, EnumDifficulty.PEACEFUL, Minecraft.getMinecraft().mcProfiler);

        WorldUtils.loadChunksClientWorld(world, BlockPos.ORIGIN, schematic.getSize());
        schematic.placeSchematicToWorld(world, BlockPos.ORIGIN, new PlacementSettings(), 2);

        AreaSelection area = new AreaSelection();
        area.setName("Converted Schematic");
        area.createNewSubRegionBox(BlockPos.ORIGIN);
        area.getSelectedSubRegionBox().setName("Converted Schematic");
        area.getSelectedSubRegionBox().setPos2(new BlockPos(schematic.getSize()));

        LitematicaSchematic litematicaSchematic = LitematicaSchematic.createFromWorld(world, area, true, "?", feedback);

        if (litematicaSchematic != null)
        {
            return litematicaSchematic.writeToFile(outputDir, outputFileName, override, feedback);
        }
        else
        {
            feedback.setString("litematica.error.schematic_conversion.schematic_to_litematica.failed_to_create_schematic");
            return false;
        }
    }

    public static boolean convertStructureToLitematicaSchematic(
            File structureDir, String structureFileName, File outputDir, String outputFileName, boolean override, IStringConsumer feedback)
    {
        DataFixer fixer = Minecraft.getMinecraft().getDataFixer();
        File file = new File(structureDir, structureFileName);
        InputStream is = null;

        try
        {
            is = new FileInputStream(file);
            Template template = readTemplateFromStream(is, fixer);
            WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
            WorldClient world = new WorldClient(null, settings, 0, EnumDifficulty.PEACEFUL, Minecraft.getMinecraft().mcProfiler);

            loadChunksClientWorld(world, BlockPos.ORIGIN, template.getSize());

            template.addBlocksToWorld(world, BlockPos.ORIGIN, null, new PlacementSettings(), 2);

            AreaSelection area = new AreaSelection();
            area.setName("Converted Structure");
            area.createNewSubRegionBox(BlockPos.ORIGIN);
            area.getSelectedSubRegionBox().setName("Converted Structure");
            area.getSelectedSubRegionBox().setPos2(new BlockPos(template.getSize()));

            LitematicaSchematic schematic = LitematicaSchematic.createFromWorld(world, area, true, "?", feedback);

            if (schematic != null)
            {
                return schematic.writeToFile(outputDir, outputFileName, override, feedback);
            }
            else
            {
                feedback.setString("litematica.error.schematic_conversion.structure_to_litematica_failed");
            }
        }
        catch (Throwable var10)
        {
        }
        finally
        {
            IOUtils.closeQuietly(is);
        }

        return false;
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
}
