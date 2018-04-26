package fi.dy.masa.litematica.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WidgetCheckBox
{
    protected final Minecraft mc;
    protected final int x;
    protected final int y;
    protected final int totalWidth;
    protected final int totalHeight;
    protected final int textWidth;
    protected final float zLevel;
    protected final String displayText;
    protected final Widgets widgetUnchecked;
    protected final Widgets widgetChecked;
    protected boolean checked;

    public WidgetCheckBox(int x, int y, float zLevel, Widgets widgetUnchecked, Widgets widgetChecked, String text, Minecraft mc)
    {
        this.x = x;
        this.y = y;
        this.zLevel = zLevel;
        this.textWidth = mc.fontRenderer.getStringWidth(text);
        this.totalWidth = this.textWidth + widgetChecked.getWidth() + 3;
        this.totalHeight = Math.max(mc.fontRenderer.FONT_HEIGHT, widgetChecked.getHeight());
        this.widgetUnchecked = widgetUnchecked;
        this.widgetChecked = widgetChecked;
        this.displayText = text;
        this.mc = mc;
    }

    public boolean isChecked()
    {
        return this.checked;
    }

    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }

    public boolean isMouseOver(int mouseX, int mouseY)
    {
        return mouseX >= this.x && mouseX < this.x + this.totalWidth &&
               mouseY >= this.y && mouseY < this.y + this.totalHeight;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.isMouseOver(mouseX, mouseY))
        {
            this.checked = ! this.checked;
            return true;
        }

        return false;
    }

    public void render()
    {
        GlStateManager.color(1f, 1f, 1f);
        this.mc.getTextureManager().bindTexture(Widgets.TEXTURE);

        Widgets widget = this.checked ? this.widgetChecked : this.widgetUnchecked;
        widget.renderAt(this.x, this.y, this.zLevel);

        int iw = widget.getWidth();
        int y = this.y + this.totalHeight / 2 - this.mc.fontRenderer.FONT_HEIGHT;
        int textColor = this.checked ? 0xFFFFFFFF : 0xCCCCCCCC;

        this.mc.fontRenderer.drawString(this.displayText, this.x + iw + 3, y, textColor);
    }
}
