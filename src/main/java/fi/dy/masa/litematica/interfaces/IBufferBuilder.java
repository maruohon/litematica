package fi.dy.masa.litematica.interfaces;

public interface IBufferBuilder
{
    int getColorIndexAccessor(int vertexIndex);

    void putColorRGBA(int index, int red, int green, int blue, int alpha);
}
