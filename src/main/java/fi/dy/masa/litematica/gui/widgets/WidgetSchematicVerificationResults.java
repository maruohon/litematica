package fi.dy.masa.litematica.gui.widgets;

import java.util.List;
import fi.dy.masa.litematica.data.SchematicVerifier.BlockMismatch;
import fi.dy.masa.litematica.data.SchematicVerifier.MismatchType;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier;
import fi.dy.masa.litematica.gui.GuiSchematicVerifier.BlockMismatchEntry;
import fi.dy.masa.litematica.gui.widgets.base.WidgetListBase;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class WidgetSchematicVerificationResults extends WidgetListBase<BlockMismatchEntry, WidgetSchematicVerificationResult>
{
    private static final String BOLD = TextFormatting.BOLD.toString();
    private static final String RED = TextFormatting.RED.toString();
    private static final String RESET = TextFormatting.RESET.toString();
    private static final String WHITE = TextFormatting.WHITE.toString();

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

        String strExpected = WHITE + BOLD + I18n.format("litematica.gui.label.schematic_verifier.expected") + RESET;
        String strFound = WHITE + BOLD + I18n.format("litematica.gui.label.schematic_verifier.found") + RESET;
        this.listContents.add(new BlockMismatchEntry(null, strExpected, strFound));

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

        this.scrollBar.setMaxValue(this.listContents.size() - this.maxVisibleBrowserEntries);

        this.updateBrowserMaxVisibleEntries();
        this.recreateListWidgets();
    }

    private void addEntriesForType(MismatchType type)
    {
        String title = RED + BOLD + type.getDisplayname() + RESET;
        this.listContents.add(new BlockMismatchEntry(null, title, null));

        List<BlockMismatch> list = this.guiSchematicVerifier.getPlacement().getSchematicVerifier().getMismatchOverviewFor(type);

        for (BlockMismatch entry : list)
        {
            this.listContents.add(new BlockMismatchEntry(entry, null, null));
        }
    }

    @Override
    protected WidgetSchematicVerificationResult createListWidget(int x, int y, boolean isOdd, BlockMismatchEntry entry)
    {
        return new WidgetSchematicVerificationResult(x, y, this.browserEntryWidth, this.browserEntryHeight, this.zLevel, isOdd, entry);
    }
}
