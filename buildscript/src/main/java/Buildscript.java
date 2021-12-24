import io.github.coolcrabs.brachyura.decompiler.BrachyuraDecompiler;
import io.github.coolcrabs.brachyura.fabric.FabricLoader;
import io.github.coolcrabs.brachyura.fabric.FabricMaven;
import io.github.coolcrabs.brachyura.fabric.FabricProject;
import io.github.coolcrabs.brachyura.fabric.Yarn;
import io.github.coolcrabs.brachyura.maven.MavenId;
import net.fabricmc.mappingio.tree.MappingTree;

public class Buildscript extends FabricProject {
    @Override
    public String getMcVersion() {
        return "1.18";
    }

    @Override
    public String getModId() {
        return "litematica-fabric";
    }

    @Override
    public String getVersion() {
        return "0.0.0-dev";
    }

    @Override
    public MappingTree createMappings() {
        return Yarn.ofMaven(FabricMaven.URL, FabricMaven.yarn("1.18+build.1")).tree;
    }

    @Override
    public FabricLoader getLoader() {
        return new FabricLoader(FabricMaven.URL, FabricMaven.loader("0.12.8"));
    }

    @Override
    public void getModDependencies(ModDependencyCollector d) {
        d.addMaven("https://maven.terraformersmc.com/releases/", new MavenId("com.terraformersmc:modmenu:3.0.0"), ModDependencyFlag.COMPILE);
        d.addMaven("https://masa.dy.fi/maven", new MavenId("fi.dy.masa.malilib:malilib-fabric-1.18.0:0.10.0-dev.26"), ModDependencyFlag.RUNTIME, ModDependencyFlag.COMPILE);
    }

    @Override
    public BrachyuraDecompiler decompiler() {
        return null;
    }

    @Override
    public int getJavaVersion() {
        return 17;
    }
}