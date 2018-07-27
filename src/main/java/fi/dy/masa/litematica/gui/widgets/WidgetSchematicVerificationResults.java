package fi.dy.masa.litematica.gui.widgets;

import java.util.List;
import fi.dy.masa.litematica.data.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import net.minecraft.client.resources.I18n;

public class WidgetSchematicVerificationResults extends WidgetListBase<BlockMismatchEntry, WidgetSchematicVerificationResult>
{
    private final GuiSchematicVerifier guiSchematicVerifier;

    public WidgetSchematicVerificationResults(int x, int y, int width, int height, GuiSchematicVerifier parent)
    {
        super(x, y, width, height, parent);

        this.browserEntryHeight = 22;
        this.guiSchematicVerifier = parent;
        this.setParent(parent);
    }

    @Override
    public void refreshEntries()
    {
        this.listContents.clear();

        String strExpected = TXT_WHITE + TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.expected") + TXT_RST;
        String strFound = TXT_WHITE + TXT_BOLD + I18n.format("litematica.gui.label.schematic_verifier.found") + TXT_RST;
        this.listContents.add(new BlockMismatchEntry(strExpected, strFound));

        MismatchType type = this.guiSchematicVerifier.getResultMode();

        if (type == MismatchType.ALL)
        {
            this.addEntriesForType(MismatchType.WRONG_BLOCK);
            this.addEntriesForType(MismatchType.WRONG_STATE);
            this.addEntriesForType(MismatchType.EXTRA);
            this.addEntriesForType(MismatchType.MISSING);
        }
        else
        {
            this.addEntriesForType(type);
        }

        this.recreateListWidgets();
    }

    private void addEntriesForType(MismatchType type)
    {
        String title = type.getFormattingCode() + type.getDisplayname() + TXT_RST;
        this.listContents.add(new BlockMismatchEntry(type, title));

        List<BlockMismatch> list = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getMismatchOverviewFor(type);

        for (BlockMismatch entry : list)
        {
            this.listContents.add(new BlockMismatchEntry(type, entry));
        }
    }

    @Override
    protected WidgetSchematicVerificationResult createListWidget(int x, int y, boolean isOdd, BlockMismatchEntry entry)
    {
        return new WidgetSchematicVerificationResult(x, y, this.browserEntryWidth, this.getBrowserEntryHeightFor(entry), this.zLevel, isOdd, entry, this.guiSchematicVerifier);
    }
}
