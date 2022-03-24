package fi.dy.masa.litematica.gui.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.malilib.gui.icon.FileBrowserIconProvider;
import fi.dy.masa.malilib.gui.icon.MultiIcon;
import fi.dy.masa.litematica.schematic.SchematicType;

public class SchematicBrowserIconProvider implements FileBrowserIconProvider
{
    protected final HashMap<File, MultiIcon> cachedIcons = new HashMap<>();

    @Override
    @Nullable
    public MultiIcon getIconForFile(File file)
    {
        MultiIcon icon = this.cachedIcons.get(file);

        if (icon == null && this.cachedIcons.containsKey(file) == false)
        {
            List<SchematicType<?>> possibleTypes = SchematicType.getPossibleTypesFromFileName(file);

            if (possibleTypes.isEmpty() == false)
            {
                icon = possibleTypes.get(0).getIcon();
            }

            this.cachedIcons.put(file, icon);
        }

        return icon;
    }

    public void setIconForFile(File file, @Nullable MultiIcon icon)
    {
        this.cachedIcons.put(file, icon);
    }
}
