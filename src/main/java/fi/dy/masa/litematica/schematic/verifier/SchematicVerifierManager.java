package fi.dy.masa.litematica.schematic.verifier;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;

public class SchematicVerifierManager
{
    public static final SchematicVerifierManager INSTANCE = new SchematicVerifierManager();

    protected final ArrayList<SchematicVerifier> activeVerifiers = new ArrayList<>();
    protected final ArrayList<SchematicVerifier> allVerifiers = new ArrayList<>();
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
        if (this.selectedVerifier == null)
        {
            SchematicPlacement placement = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

            if (placement != null)
            {
                // TODO
                this.activeVerifiers.clear();
                SchematicVerifier verifier = new SchematicVerifier(placement);
                //this.allVerifiers.add(verifier);
                this.selectedVerifier = verifier;
                this.activeVerifiers.add(verifier);
            }
        }

        return this.selectedVerifier;
    }

    public SchematicVerifier getOrCreateVerifierForPlacement(SchematicPlacement placement)
    {
        if (this.selectedVerifier != null)
        {
            this.selectedVerifier.reset();
        }

        // TODO
        SchematicVerifier verifier = new SchematicVerifier(placement);
        //this.allVerifiers.add(verifier);
        this.selectedVerifier = verifier;
        this.activeVerifiers.clear();
        this.activeVerifiers.add(verifier);

        return verifier;
    }

    public void setSelectedVerifier(@Nullable SchematicVerifier selectedVerifier)
    {
        this.selectedVerifier = selectedVerifier;
    }
}
