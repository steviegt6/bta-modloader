package dev.tomat.modloader;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.*;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.metadata.EntrypointMetadata;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import net.fabricmc.loader.impl.metadata.NestedJarEntry;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ModLoaderModMetadata implements LoaderModMetadata {
    private Collection<ModDependency> dependencies;
    private final String id;
    private final String version;

    public ModLoaderModMetadata(String id, String version) {
        this.id = id;
        this.version = version;
    }

    @Override
    public int getSchemaVersion() {
        return 1;
    }

    @Override
    public String getType() {
        return "modloader";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Collection<String> getProvides() {
        return Collections.emptyList();
    }

    @Override
    public Version getVersion() {
        try {
            return Version.parse(version);
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setVersion(Version version) { }

    @Override
    public ModEnvironment getEnvironment() {
        return ModEnvironment.UNIVERSAL;
    }

    @Override
    public boolean loadsInEnvironment(EnvType type) {
        return true;
    }

    @Override
    public Collection<ModDependency> getDependencies() {
        return this.dependencies;
    }

    @Override
    public void setDependencies(Collection<ModDependency> dependencies) {
        this.dependencies = Collections.unmodifiableCollection(dependencies);
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Collection<Person> getAuthors() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Person> getContributors() {
        return Collections.emptyList();
    }

    @Override
    public ContactInformation getContact() {
        return new ContactInformationImpl(new HashMap<>());
    }

    @Override
    public Collection<String> getLicense() {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getIconPath(int size){
        return Optional.empty();
    }

    @Override
    public boolean containsCustomValue(String s) {
        return false;
    }

    @Override
    public CustomValue getCustomValue(String s) {
        return null;
    }

    @Override
    public Map<String, CustomValue> getCustomValues() {
        return Collections.emptyMap();
    }

    @Override
    public boolean containsCustomElement(String s) {
        return false;
    }

    @Override
    public Map<String, String> getLanguageAdapterDefinitions() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<NestedJarEntry> getJars() {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getMixinConfigs(EnvType type) {
        return Collections.emptyList();
    }

    @Override
    public String getAccessWidener() {
        return null;
    }

    @Override
    public Collection<String> getOldInitializers() {
        return Collections.emptyList();
    }

    @Override
    public List<EntrypointMetadata> getEntrypoints(String type) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getEntrypointKeys() {
        return Collections.emptyList();
    }

    @Override
    public void emitFormatWarnings() { }
}
