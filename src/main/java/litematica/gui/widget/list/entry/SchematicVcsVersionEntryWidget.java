package litematica.gui.widget.list.entry;

import malilib.gui.widget.list.entry.BaseDataListEntryWidget;
import malilib.gui.widget.list.entry.DataListEntryWidgetData;
import malilib.render.text.StyledTextLine;
import litematica.schematic.projects.SchematicVersion;

public class SchematicVcsVersionEntryWidget extends BaseDataListEntryWidget<SchematicVersion>
{
    public SchematicVcsVersionEntryWidget(SchematicVersion data, DataListEntryWidgetData constructData)
    {
        super(data, constructData);

        String key = "litematica.label.widget.schematic_vcs.version_entry";
        this.setText(StyledTextLine.translateFirstLine(key, data.getVersion(), data.getName()));
    }

    @Override
    protected boolean isSelected()
    {
        return this.data.getProject().getCurrentVersion() == this.data;
    }
}
