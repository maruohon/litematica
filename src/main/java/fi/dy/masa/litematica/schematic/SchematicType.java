package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.litematica.gui.LitematicaGuiIcons;
import fi.dy.masa.malilib.gui.interfaces.IGuiIcon;
import fi.dy.masa.malilib.util.NBTUtils;

public class SchematicType<S extends ISchematic>
{
    public static final SchematicType<LitematicaSchematic> LITEMATICA = new SchematicType<LitematicaSchematic>(
            LitematicaSchematic::new,
            LitematicaSchematic::isValidSchematic,
            LitematicaSchematic.FILE_NAME_EXTENSION,
            LitematicaSchematic.FILE_NAME_EXTENSION::equals,
            LitematicaGuiIcons.FILE_ICON_LITEMATIC);

    public static final SchematicType<SchematicaSchematic> SCHEMATICA = new SchematicType<SchematicaSchematic>(
            SchematicaSchematic::new,
            SchematicaSchematic::isValidSchematic,
            SchematicaSchematic.FILE_NAME_EXTENSION,
            SchematicaSchematic.FILE_NAME_EXTENSION::equals,
            LitematicaGuiIcons.FILE_ICON_SCHEMATIC);

    public static final SchematicType<SpongeSchematic> SPONGE = new SchematicType<SpongeSchematic>(
            SpongeSchematic::new,
            SpongeSchematic::isValidSchematic,
            SpongeSchematic.FILE_NAME_EXTENSION,
            (ext) -> { return SpongeSchematic.FILE_NAME_EXTENSION.equals(ext) || SchematicaSchematic.FILE_NAME_EXTENSION.equals(ext); },
            LitematicaGuiIcons.FILE_ICON_SPONGE);

    public static final ImmutableList<SchematicType<?>> KNOWN_TYPES = ImmutableList.of(LITEMATICA, SCHEMATICA, SPONGE);

    private final String extension;
    private final IGuiIcon icon;
    private final Function<File, S> factory;
    private final Function<String, Boolean> extensionValidator;
    private final Function<NBTTagCompound, Boolean> dataValidator;

    public SchematicType(Function<File, S> factory, Function<NBTTagCompound, Boolean> dataValidator,
            String extension, Function<String, Boolean> extensionValidator, IGuiIcon icon)
    {
        this.extension = extension;
        this.factory = factory;
        this.extensionValidator = extensionValidator;
        this.dataValidator = dataValidator;
        this.icon = icon;
    }

    public String getFileNameExtension()
    {
        return this.extension;
    }

    public IGuiIcon getIcon()
    {
        return this.icon;
    }

    public boolean isValidExtension(String extension)
    {
        return this.extensionValidator.apply(extension).booleanValue();
    }

    public boolean isValidData(NBTTagCompound tag)
    {
        return this.dataValidator.apply(tag).booleanValue();
    }

    public S createSchematic(@Nullable File file)
    {
        return this.factory.apply(file);
    }

    @Nullable
    public S createSchematicAndReadFromTag(@Nullable File file, NBTTagCompound tag)
    {
        S schematic = this.factory.apply(file);

        if (schematic.fromTag(tag))
        {
            return schematic;
        }

        return null;
    }

    public static List<SchematicType<?>> getPossibleTypesFromFileName(File file)
    {
        String extension = "." + org.apache.logging.log4j.core.util.FileUtils.getFileExtension(file);
        List<SchematicType<?>> list = new ArrayList<>();

        for (SchematicType<?> type : KNOWN_TYPES)
        {
            if (type.isValidExtension(extension))
            {
                list.add(type);
            }
        }

        return list;
    }

    @Nullable
    public static SchematicType<?> getType(File file, NBTTagCompound tag)
    {
        List<SchematicType<?>> possibleTypes = getPossibleTypesFromFileName(file);

        if (possibleTypes.isEmpty() == false)
        {
            for (SchematicType<?> type : possibleTypes)
            {
                if (type.isValidData(tag))
                {
                    return type;
                }
            }
        }

        return null;
    }

    @Nullable
    public static ISchematic tryCreateSchematicFrom(File file)
    {
        List<SchematicType<?>> possibleTypes = getPossibleTypesFromFileName(file);

        if (possibleTypes.isEmpty() == false)
        {
            NBTTagCompound tag = NBTUtils.readNbtFromFile(file);
            SchematicType<?> type = getType(file, tag);
            return type != null ? type.createSchematicAndReadFromTag(file, tag) : null;
        }

        return null;
    }

    @Nullable
    public static ISchematic tryCreateSchematicFrom(File file, NBTTagCompound tag)
    {
        SchematicType<?> type = getType(file, tag);
        return type != null ? type.createSchematicAndReadFromTag(file, tag) : null;
    }
}
