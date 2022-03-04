package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.litematica.gui.util.LitematicaIcons;
import fi.dy.masa.malilib.gui.icon.MultiIcon;
import fi.dy.masa.malilib.util.FileNameUtils;
import fi.dy.masa.malilib.util.nbt.NbtUtils;

public class SchematicType<S extends ISchematic>
{
    public static final SchematicType<LitematicaSchematic> LITEMATICA = SchematicType.<LitematicaSchematic>builder()
            .setDisplayName("Litematica")
            .setFactory(LitematicaSchematic::new)
            .setDataValidator(LitematicaSchematic::isValidSchematic)
            .setExtension(LitematicaSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator(LitematicaSchematic.FILE_NAME_EXTENSION::equals)
            .setIcon(LitematicaIcons.FILE_ICON_LITEMATIC)
            .setHasName(true)
            .build();

    public static final SchematicType<SchematicaSchematic> SCHEMATICA = SchematicType.<SchematicaSchematic>builder()
            .setDisplayName("Schematica/MCEdit")
            .setFactory(SchematicaSchematic::new)
            .setDataValidator(SchematicaSchematic::isValidSchematic)
            .setExtension(SchematicaSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator(SchematicaSchematic.FILE_NAME_EXTENSION::equals)
            .setIcon(LitematicaIcons.FILE_ICON_SCHEMATIC)
            .setHasName(true)
            .build();

    public static final SchematicType<SpongeSchematic> SPONGE = SchematicType.<SpongeSchematic>builder()
            .setDisplayName("Sponge")
            .setFactory(SpongeSchematic::new)
            .setDataValidator(SpongeSchematic::isValidSchematic)
            .setExtension(SpongeSchematic.FILE_NAME_EXTENSION)
            .setExtensionValidator((ext) -> SpongeSchematic.FILE_NAME_EXTENSION.equals(ext) || SchematicaSchematic.FILE_NAME_EXTENSION.equals(ext))
            .setIcon(LitematicaIcons.FILE_ICON_SPONGE)
            .setHasName(true)
            .build();

    public static final SchematicType<VanillaStructure> VANILLA = SchematicType.<VanillaStructure>builder()
            .setDisplayName("Vanilla Structure")
            .setFactory(VanillaStructure::new)
            .setDataValidator(VanillaStructure::isValidSchematic)
            .setExtension(VanillaStructure.FILE_NAME_EXTENSION)
            .setExtensionValidator(VanillaStructure.FILE_NAME_EXTENSION::equals)
            .setIcon(LitematicaIcons.FILE_ICON_VANILLA)
            .setHasName(true)
            .build();

    public static final ImmutableList<SchematicType<?>> KNOWN_TYPES = ImmutableList.of(LITEMATICA, SCHEMATICA, SPONGE, VANILLA);

    public static final FileFilter SCHEMATIC_FILE_FILTER = (file) -> file.isFile() && file.canRead() && getPossibleTypesFromFileName(file).isEmpty() == false;

    private final String extension;
    private final MultiIcon icon;
    private final Function<File, S> factory;
    private final Function<String, Boolean> extensionValidator;
    private final Function<NBTTagCompound, Boolean> dataValidator;
    private final String displayName;
    private final boolean hasName;

    private SchematicType(String displayName, Function<File, S> factory, Function<NBTTagCompound, Boolean> dataValidator,
                          String extension, Function<String, Boolean> extensionValidator, MultiIcon icon, boolean hasName)
    {
        this.displayName = displayName;
        this.extension = extension;
        this.factory = factory;
        this.extensionValidator = extensionValidator;
        this.dataValidator = dataValidator;
        this.icon = icon;
        this.hasName = hasName;
    }

    public String getFileNameExtension()
    {
        return this.extension;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public MultiIcon getIcon()
    {
        return this.icon;
    }

    public boolean getHasName()
    {
        return this.hasName;
    }

    public boolean isValidExtension(String extension)
    {
        return this.extensionValidator.apply(extension).booleanValue();
    }

    public boolean isValidData(NBTTagCompound tag)
    {
        return this.dataValidator.apply(tag).booleanValue();
    }

    /**
     * Creates a new schematic, with the provided file passed to the constructor of the schematic.
     * This does not read anything from the file.
     * @param file
     * @return
     */
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
        return getPossibleTypesFromFileName(file.getName());
    }

    public static List<SchematicType<?>> getPossibleTypesFromFileName(String fileName)
    {
        String extension = "." + FileNameUtils.getFileNameExtension(fileName.toLowerCase(Locale.ROOT));
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
            NBTTagCompound tag = NbtUtils.readNbtFromFile(file);

            if (tag != null)
            {
                SchematicType<?> type = getType(file, tag);

                if (type != null)
                {
                    return type.createSchematicAndReadFromTag(file, tag);
                }
            }
        }

        return null;
    }

    @Nullable
    public static ISchematic tryCreateSchematicFrom(File file, NBTTagCompound tag)
    {
        SchematicType<?> type = getType(file, tag);
        return type != null ? type.createSchematicAndReadFromTag(file, tag) : null;
    }

    public static <S extends ISchematic> Builder<S> builder()
    {
        return new Builder<S>();
    }

    public static class Builder<S extends ISchematic>
    {
        private String extension = null;
        private MultiIcon icon = null;
        private Function<File, S> factory = null;
        private Function<String, Boolean> extensionValidator = null;
        private Function<NBTTagCompound, Boolean> dataValidator = null;
        private String displayName = "?";
        private boolean hasName = false;

        public Builder<S> setDataValidator(Function<NBTTagCompound, Boolean> dataValidator)
        {
            this.dataValidator = dataValidator;
            return this;
        }

        public Builder<S> setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }

        public Builder<S> setExtension(String extension)
        {
            this.extension = extension;
            return this;
        }

        public Builder<S> setExtensionValidator(Function<String, Boolean> extensionValidator)
        {
            this.extensionValidator = extensionValidator;
            return this;
        }

        public Builder<S> setFactory(Function<File, S> factory)
        {
            this.factory = factory;
            return this;
        }

        public Builder<S> setHasName(boolean hasName)
        {
            this.hasName = hasName;
            return this;
        }

        public Builder<S> setIcon(MultiIcon icon)
        {
            this.icon = icon;
            return this;
        }

        public SchematicType<S> build()
        {
            return new SchematicType<S>(this.displayName, this.factory, this.dataValidator, this.extension, this.extensionValidator, this.icon, this.hasName);
        }
    }
}
