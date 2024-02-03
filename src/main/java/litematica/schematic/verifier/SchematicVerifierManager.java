package litematica.schematic.verifier;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import litematica.data.DataManager;
import litematica.schematic.placement.SchematicPlacement;

public class SchematicVerifierManager
{
    public static final SchematicVerifierManager INSTANCE = new SchematicVerifierManager();

    protected final ArrayList<SchematicVerifier> activeVerifiers = new ArrayList<>();
    protected final ArrayList<SchematicVerifier> allVerifiers = new ArrayList<>();
    protected final LongOpenHashSet touchedChunks = new LongOpenHashSet();
    protected final LongOpenHashSet reCheckChunks = new LongOpenHashSet();
    @Nullable protected SchematicVerifier selectedVerifier;

    public List<SchematicVerifier> getActiveVerifiers()
    {
        return this.activeVerifiers;
    }

    @Nullable
    public SchematicVerifier getSelectedVerifier()
    {
        return this.selectedVerifier;
    }

    @Nullable
    public SchematicVerifier getSelectedVerifierOrCreateForPlacement()
    {
        // TODO

        /*
        if (this.selectedVerifier == null)
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                this.createAndAddVerifier(placement);
            }
        }
        */

        SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (placement != null)
        {
            this.getOrCreateVerifierForPlacement(placement);
        }

        return this.selectedVerifier;
    }

    public SchematicVerifier getOrCreateVerifierForPlacement(SchematicPlacement placement)
    {
        if (this.selectedVerifier != null)
        {
            if (this.selectedVerifier.hasPlacement(placement))
            {
                return this.selectedVerifier;
            }

            this.selectedVerifier.reset();
        }

        // TODO
        this.createAndAddVerifier(placement);
        this.updateTouchedChunks();

        return this.selectedVerifier;
    }

    public void setSelectedVerifier(@Nullable SchematicVerifier selectedVerifier)
    {
        this.selectedVerifier = selectedVerifier;
    }

    public void onPlacementRemoved(SchematicPlacement placement)
    {
        this.activeVerifiers.removeIf((v) -> v.removePlacement(placement));

        if (this.selectedVerifier != null &&
            this.activeVerifiers.contains(this.selectedVerifier) == false)
        {
            this.selectedVerifier = null;
        }
    }

    public void onBlockChanged(BlockPos pos)
    {
        this.onChunkChanged(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public void onChunkChanged(int cx, int cz)
    {
        long posLong = ChunkPos.asLong(cx, cz);

        if (this.touchedChunks.contains(posLong))
        {
            this.reCheckChunks.add(posLong);
        }
    }

    public void scheduleReChecks()
    {
        if (this.reCheckChunks.isEmpty() == false)
        {
            for (SchematicVerifier verifier : this.activeVerifiers)
            {
                verifier.reCheckChunks(this.reCheckChunks);
            }
        }

        this.reCheckChunks.clear();
    }

    protected void createAndAddVerifier(SchematicPlacement placement)
    {
        // TODO
        this.activeVerifiers.clear();
        SchematicVerifier verifier = new SchematicVerifier(placement);
        this.selectedVerifier = verifier;
        this.activeVerifiers.add(verifier);
    }

    public void updateTouchedChunks()
    {
        this.touchedChunks.clear();

        for (SchematicVerifier verifier : this.activeVerifiers)
        {
            for (ChunkPos pos : verifier.getTouchedChunks())
            {
                long posLong = ChunkPos.asLong(pos.x, pos.z);
                this.touchedChunks.add(posLong);
            }
        }
    }
}
