package litematica.interfaces;

public interface IWorldUpdateSuppressor
{
    boolean getShouldPreventUpdates();

    void setShouldPreventUpdates(boolean preventUpdates);
}
