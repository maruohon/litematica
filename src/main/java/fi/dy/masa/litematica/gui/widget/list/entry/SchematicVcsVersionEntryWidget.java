package fi.dy.masa.litematica.gui.widget.list.entry;

import fi.dy.masa.litematica.schematic.projects.SchematicVersion;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.render.text.StyledTextLine;

public class SchematicVcsVersionEntryWidget extends BaseDataListEntryWidget<SchematicVersion>
{
    public SchematicVcsVersionEntryWidget(SchematicVersion data, DataListEntryWidgetData constructData)
    {
        super(data, constructData);

        String key = "litematica.label.widget.schematic_vcs.version_entry";
        this.setText(StyledTextLine.translate(key, data.getVersion(), data.getName()));
    }

    @Override
    protected boolean isSelected()
    {
        return this.data.getProject().getCurrentVersion() == this.data;
    }
}
