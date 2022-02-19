package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorSubRegion;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.ButtonActionListener;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.list.entry.BaseListEntryWidget;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.render.TextRenderUtils;
import fi.dy.masa.malilib.util.data.ResultingStringConsumer;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;

public class WidgetSelectionSubRegion extends BaseListEntryWidget<String>
{
    private final WidgetListSelectionSubRegions parent;
    private final AreaSelection selection;
    private final SelectionBox box;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSelectionSubRegion(int x, int y, int width, int height, boolean isOdd,
            String entry, int listIndex, AreaSelection selection, WidgetListSelectionSubRegions parent)
    {
        super(x, y, width, height, entry, listIndex);

        this.selection = selection;
        this.box = selection.getSubRegionBox(entry);
        this.isOdd = isOdd;
        this.parent = parent;

        int posX = x + width - 2;
        int posY = y + 1;

        posX = this.createButton(posX, posY, ButtonListener.ButtonType.REMOVE);
        posX = this.createButton(posX, posY, ButtonListener.ButtonType.RENAME);
        posX = this.createButton(posX, posY, ButtonListener.ButtonType.CONFIGURE);

        this.buttonsStartX = posX;
    }

    private int createButton(int x, int y, ButtonListener.ButtonType type)
    {
        return this.addButton(new GenericButton(x, y, -1, true, type.getDisplayName()), new ButtonListener(type, this)).getX() - 1;
    }

    @Override
    public boolean canHoverAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canHoverAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean isActiveGui, boolean hovered)
    {
        RenderUtils.color(1f, 1f, 1f, 1f);

        boolean selected = this.entry.equals(this.selection.getCurrentSubRegionBoxName());
        int x = this.getX();
        int y = this.getY();
        int z = this.getZ();
        int width = this.getWidth();
        int height = this.getHeight();

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            ShapeRenderUtils.renderRectangle(x, y, z, width, height, 0xA0303030);
        }

        if (selected)
        {
            ShapeRenderUtils.renderOutline(x, y, z + 1, width, height, 1, 0xFFE0E0E0);
        }

        this.drawString(x + 2, y + this.getCenteredTextOffsetY(), 0xFFFFFFFF, this.entry);

        super.render(mouseX, mouseY, isActiveGui, hovered);

        RenderUtils.color(1f, 1f, 1f, 1f);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean isActiveGui, int hoveredWidgetId)
    {
        List<String> text = new ArrayList<>();

        if (this.box != null)
        {
            BlockPos pos1 = this.box.getPos1();
            BlockPos pos2 = this.box.getPos2();

            if (pos1 != null)
            {
                String str = StringUtils.translate("litematica.gui.label.area_editor.pos1");
                text.add(String.format("%s: x: %d, y: %d, z: %d", str, pos1.getX(), pos1.getY(), pos1.getZ()));
            }

            if (pos2 != null)
            {
                String str = StringUtils.translate("litematica.gui.label.area_editor.pos2");
                text.add(String.format("%s: x: %d, y: %d, z: %d", str, pos2.getX(), pos2.getY(), pos2.getZ()));
            }

            if (pos1 != null && pos2 != null)
            {
                String str = StringUtils.translate("litematica.gui.label.area_editor.dimensions");
                Vec3i size = PositionUtils.getAreaSizeFromRelativeEndPosition(pos2.subtract(pos1));
                text.add(String.format("%s: %d x %d x %d", str, Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ())));
            }
        }

        int offset = 12;

        if (BaseScreen.isMouseOver(mouseX, mouseY, this.getX(), this.getY(), this.buttonsStartX - offset, this.getHeight()))
        {
            TextRenderUtils.renderHoverText(mouseX, mouseY, this.getZ() + 1, text);
        }

        RenderUtils.color(1f, 1f, 1f, 1f);
    }

    private static class ButtonListener implements ButtonActionListener
    {
        private final WidgetSelectionSubRegion widget;
        private final ButtonType type;

        public ButtonListener(ButtonType type, WidgetSelectionSubRegion widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformedWithButton(BaseButton button, int mouseButton)
        {
            if (this.type == ButtonType.RENAME)
            {
                String title = "litematica.gui.title.rename_area_sub_region";
                String name = this.widget.box != null ? this.widget.box.getName() : "<error>";
                BoxRenamer renamer = new BoxRenamer(this.widget.selection, this.widget);
                BaseScreen.openPopupScreen(new TextInputScreen(title, name, this.widget.parent.getEditorGui(), renamer));
            }
            else if (this.type == ButtonType.REMOVE)
            {
                this.widget.selection.removeSubRegionBox(this.widget.entry);
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.CONFIGURE)
            {
                GuiAreaSelectionEditorSubRegion gui = new GuiAreaSelectionEditorSubRegion(this.widget.selection, this.widget.box);
                gui.setParent(GuiUtils.getCurrentScreen());
                BaseScreen.openScreen(gui);
            }
        }

        public enum ButtonType
        {
            RENAME          ("litematica.gui.button.rename"),
            CONFIGURE       ("litematica.gui.button.configure"),
            REMOVE          (BaseScreen.TXT_RED + "-");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getDisplayName()
            {
                return StringUtils.translate(this.labelKey);
            }
        }
    }

    private static class BoxRenamer implements ResultingStringConsumer
    {
        private final WidgetSelectionSubRegion widget;
        private final AreaSelection selection;

        public BoxRenamer(AreaSelection selection, WidgetSelectionSubRegion widget)
        {
            this.widget = widget;
            this.selection = selection;
        }

        @Override
        public boolean consumeString(String string)
        {
            String oldName = this.widget.entry;
            return this.selection.renameSubRegionBox(oldName, string, this.widget.parent.getEditorGui());
        }
    }
}
