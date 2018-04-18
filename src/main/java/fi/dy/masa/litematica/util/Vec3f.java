package fi.dy.masa.litematica.util;

public class Vec3f
{
    public static final Vec3f ZERO = new Vec3f(0F, 0F, 0F);
    public final float x;
    public final float y;
    public final float z;

    public Vec3f(float xIn, float yIn, float zIn)
    {
        if (xIn == -0.0F)
        {
            xIn = 0.0F;
        }

        if (yIn == -0.0F)
        {
            yIn = 0.0F;
        }

        if (zIn == -0.0F)
        {
            zIn = 0.0F;
        }

        this.x = xIn;
        this.y = yIn;
        this.z = zIn;
    }
}
