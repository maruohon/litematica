package fi.dy.masa.litematica.util;

public class Vec4f
{
    public static final Vec4f ZERO = new Vec4f(0F, 0F, 0F, 0F);
    public final float r;
    public final float g;
    public final float b;
    public final float a;

    public Vec4f(float r, float g, float b)
    {
        this(r, g, b, 1f);
    }

    public Vec4f(float r, float g, float b, float a)
    {
        if (r == -0.0F)
        {
            r = 0.0F;
        }

        if (g == -0.0F)
        {
            g = 0.0F;
        }

        if (b == -0.0F)
        {
            b = 0.0F;
        }

        if (a == -0.0F)
        {
            a = 0.0F;
        }

        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public static Vec4f fromColor(int color)
    {
        float alpha = ((color & 0xFF000000) >>> 24) / 255f;
        return fromColor(color, alpha);
    }

    public static Vec4f fromColor(int color, float alpha)
    {
        float r = ((color & 0x00FF0000) >>> 16) / 255f;
        float g = ((color & 0x000FF000) >>>  8) / 255f;
        float b = ((color & 0x000000FF)       ) / 255f;

        return new Vec4f(r, g, b, alpha);
    }
}
