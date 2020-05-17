package fi.dy.masa.litematica.interfaces;

public interface IWorldUpdateSuppressor
{
    boolean getShouldPreventUpdates();

    void setShouldPreventUpdates(boolean preventUpdates);
}
