package fi.dy.masa.litematica.gui.widgets;

import java.util.ArrayList;
import java.util.List;
import fi.dy.masa.litematica.gui.GuiAreaSelectionEditorSubRegion;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextInputFeedback;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.interfaces.IStringConsumerFeedback;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

public class WidgetSelectionSubRegion extends WidgetListEntryBase<String>
{
    private final WidgetListSelectionSubRegions parent;
    private final AreaSelection selection;
    private final Box box;
    private final Minecraft mc;
    private final boolean isOdd;
    private final int buttonsStartX;

    public WidgetSelectionSubRegion(int x, int y, int width, int height, float zLevel, boolean isOdd,
            String entry, int listIndex, Minecraft mc, AreaSelection selection, WidgetListSelectionSubRegions parent)
    {
        super(x, y, width, height, zLevel, entry, listIndex);

        this.selection = selection;
        this.box = selection.getSubRegionBox(entry);
        this.mc = mc;
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
        String label = I18n.format(type.getLabelKey());
        int len = Math.max(this.mc.fontRenderer.getStringWidth(label) + 10, 20);
        x -= len;
        this.addButton(new ButtonGeneric(x, y, len, 20, label), new ButtonListener(type, this));

        return x - 2;
    }

    @Override
    public boolean canSelectAt(int mouseX, int mouseY, int mouseButton)
    {
        return mouseX < this.buttonsStartX && super.canSelectAt(mouseX, mouseY, mouseButton);
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected)
    {
        GlStateManager.color(1, 1, 1, 1);

        selected = this.entry.equals(this.selection.getCurrentSubRegionBoxName());

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY))
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0707070);
        }
        else if (this.isOdd)
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0101010);
        }
        // Draw a slightly lighter background for even entries
        else
        {
            GuiBase.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, 0xA0303030);
        }

        if (selected)
        {
            RenderUtils.drawOutline(this.x, this.y, this.width, this.height, 0xFFE0E0E0, 0.001f);
        }

        this.mc.fontRenderer.drawString(this.entry, this.x + 2, this.y + 7, 0xFFFFFFFF);

        super.render(mouseX, mouseY, selected);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected)
    {
        List<String> text = new ArrayList<>();

        if (this.box != null)
        {
            BlockPos pos1 = this.box.getPos1();
            BlockPos pos2 = this.box.getPos2();

            if (pos1 != null)
            {
                String str = I18n.format("litematica.gui.label.area_editor.pos1");
                text.add(String.format("%s: x: %d, y: %d, z: %d", str, pos1.getX(), pos1.getY(), pos1.getZ()));
            }

            if (pos2 != null)
            {
                String str = I18n.format("litematica.gui.label.area_editor.pos2");
                text.add(String.format("%s: x: %d, y: %d, z: %d", str, pos2.getX(), pos2.getY(), pos2.getZ()));
            }

            if (pos1 != null && pos2 != null)
            {
                String str = I18n.format("litematica.gui.label.area_editor.dimensions");
                BlockPos size = PositionUtils.getAreaSizeFromRelativeEndPosition(pos2.subtract(pos1));
                text.add(String.format("%s: %d x %d x %d", str, Math.abs(size.getX()), Math.abs(size.getY()), Math.abs(size.getZ())));
            }
        }

        int offset = 12;

        if (GuiBase.isMouseOver(mouseX, mouseY, this.x, this.y, this.buttonsStartX - offset, this.height))
        {
            RenderUtils.drawHoverText(mouseX, mouseY, text);
        }
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final WidgetSelectionSubRegion widget;
        private final ButtonType type;

        public ButtonListener(ButtonType type, WidgetSelectionSubRegion widget)
        {
            this.type = type;
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == ButtonType.RENAME)
            {
                String title = "litematica.gui.title.rename_area_sub_region";
                String name = this.widget.box != null ? this.widget.box.getName() : "<error>";
                BoxRenamer renamer = new BoxRenamer(this.widget.selection, this.widget);
                this.widget.mc.displayGuiScreen(new GuiTextInputFeedback(160, title, name, this.widget.parent.getEditorGui(), renamer));
            }
            else if (this.type == ButtonType.REMOVE)
            {
                this.widget.selection.removeSubRegionBox(this.widget.entry);
                this.widget.parent.refreshEntries();
            }
            else if (this.type == ButtonType.CONFIGURE)
            {
                GuiAreaSelectionEditorSubRegion gui = new GuiAreaSelectionEditorSubRegion(this.widget.selection, this.widget.box);
                gui.setParent(this.widget.mc.currentScreen);
                this.widget.mc.displayGuiScreen(gui);
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum ButtonType
        {
            RENAME          ("litematica.gui.button.rename"),
            CONFIGURE       ("litematica.gui.button.configure"),
            REMOVE          (TextFormatting.RED.toString() + "-");

            private final String labelKey;

            private ButtonType(String labelKey)
            {
                this.labelKey = labelKey;
            }

            public String getLabelKey()
            {
                return this.labelKey;
            }
        }
    }

    private static class BoxRenamer implements IStringConsumerFeedback
    {
        private final WidgetSelectionSubRegion widget;
        private final AreaSelection selection;

        public BoxRenamer(AreaSelection selection, WidgetSelectionSubRegion widget)
        {
            this.widget = widget;
            this.selection = selection;
        }

        @Override
        public boolean setString(String string)
        {
            String oldName = this.widget.entry;
            return this.selection.renameSubRegionBox(oldName, string, this.widget.parent.getEditorGui());
        }
    }
}
