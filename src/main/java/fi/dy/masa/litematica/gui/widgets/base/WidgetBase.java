package fi.dy.masa.litematica.gui.widgets.base;

public abstract class WidgetBase
{
    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;
    protected final float zLevel;

    public WidgetBase(int x, int y, int width, int height, float zLevel)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.zLevel = zLevel;
    }

    public boolean isMouseOver(int mouseX, int mouseY)
    {
        return mouseX >= this.x && mouseX < this.x + this.width &&
               mouseY >= this.y && mouseY < this.y + this.height;
    }

    public final boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.isMouseOver(mouseX, mouseY))
        {
            return this.onMouseClicked(mouseX, mouseY, mouseButton);
        }

        return false;
    }

    protected boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        return true;
    }

    public abstract void render(int mouseX, int mouseY, boolean selected);
}
