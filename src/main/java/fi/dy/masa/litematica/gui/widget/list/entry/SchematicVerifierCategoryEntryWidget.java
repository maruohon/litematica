package fi.dy.masa.litematica.gui.widget.list.entry;

import fi.dy.masa.malilib.gui.widget.list.entry.BaseListEntryWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.DataListEntryWidgetData;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.litematica.schematic.verifier.SchematicVerifier;
import fi.dy.masa.litematica.schematic.verifier.VerifierResultType;

public class SchematicVerifierCategoryEntryWidget extends BaseListEntryWidget
{
    protected final SchematicVerifier verifier;
    protected final VerifierResultType type;
    protected boolean selected;

    public SchematicVerifierCategoryEntryWidget(VerifierResultType type,
                                                DataListEntryWidgetData constructData,
                                                SchematicVerifier verifier,
                                                int pairCount,
                                                int positionCount)
    {
        super(constructData);

        this.type = type;
        this.verifier = verifier;

        this.selected = this.verifier.isTypeSelected(type);
        this.textSettings.setTextColor(type.getTextColor());
        this.setText(StyledTextLine.translate(type.getCategoryWidgetTranslationKey(), pairCount, positionCount));
    }

    @Override
    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (super.onMouseClicked(mouseX, mouseY, mouseButton) == false)
        {
            this.verifier.toggleTypeSelected(this.type);
            this.selected = this.verifier.isTypeSelected(this.type);
        }

        return true;
    }

    @Override
    protected boolean isSelected()
    {
        return this.selected;
    }
}
