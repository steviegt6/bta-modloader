package dev.tomat.modloader;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.metadata.LoaderModMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ModLoaderMod implements ModInitializer {
    public static final String MOD_ID = "modloader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final List<ModCandidate> modCandidates = new ArrayList<>();
    private Method loaderAddMod;
    private Constructor<ModCandidate> modCandidateConstructor;

    @Override
    public void onInitialize() {
        LOGGER.info("ModLoader initialized.");

        FabricLoader loader = FabricLoader.getInstance();
        if (!(loader instanceof FabricLoaderImpl))
            throw new IllegalStateException("FabricLoader is not an instance of FabricLoaderImpl");

        Method addMod;
        try {
            addMod = FabricLoaderImpl.class.getDeclaredMethod("addMod", ModCandidate.class);
            addMod.setAccessible(true);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("FabricLoaderImpl does not have method addMod(ModCandidate)");
        }

        loaderAddMod = addMod;

        Constructor<ModCandidate> modCandidate;
        try {
            modCandidate = ModCandidate.class.getDeclaredConstructor(List.class, String.class, long.class, LoaderModMetadata.class, boolean.class, Collection.class);
            modCandidate.setAccessible(true);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("ModCandidate does not have constructor ModCandidate(List<Path>, LoaderModMetadata, boolean, Collection<ModCandidate>)");
        }

        modCandidateConstructor = modCandidate;

        loadModLoaderMods();
    }

    private void loadModLoaderMods() {
        Path modloaderDir = FabricLoader.getInstance().getGameDir().resolve("mods").resolve("modloader");

        if (!modloaderDir.toFile().exists()) {
            LOGGER.info("ModLoader directory does not exist, creating...");
            //noinspection ResultOfMethodCallIgnored
            modloaderDir.toFile().mkdir();
            return; // Early return since there won't be any mods to load.
        }

        File[] betaVersions = modloaderDir.toFile().listFiles();
        if (betaVersions == null) {
            LOGGER.error("Failed to resolve version directories.");
            return;
        }

        for (File betaVersion : betaVersions) {
            if (!betaVersion.isDirectory()) continue;
            File[] mods = betaVersion.listFiles();
            for (File mod : mods) {
                if (!mod.getName().endsWith(".jar") && !mod.getName().endsWith(".zip")) continue;
                LOGGER.info("Loading ModLoader mod: " + mod.getName());
                String id = mod.getName().substring(0, mod.getName().length() - 4);
                addMod(createPlain(new ArrayList<>(Collections.singletonList(Paths.get(mod.getAbsolutePath()))), new ModLoaderModMetadata(id, betaVersion.getName())));
            }
        }

        ModLoaderModRemapper.remap(modCandidates, FabricLoader.getInstance().getGameDir().resolve(".fabric").resolve("tmp"), FabricLoader.getInstance().getGameDir().resolve(".fabric").resolve("processedMods"));
    }

    private void addMod(ModCandidate candidate) {
        if (loaderAddMod == null)
            throw new IllegalStateException("FabricLoaderImpl.addMod(ModCandidate) is not initialized");

        try {
            loaderAddMod.invoke(FabricLoader.getInstance(), candidate);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke FabricLoaderImpl.addMod(ModCandidate)", e);
        }

        modCandidates.add(candidate);
    }

    private ModCandidate createPlain(List<Path> paths, LoaderModMetadata metadata) {
        if (modCandidateConstructor == null)
            throw new IllegalStateException("ModCandidate constructor is not initialized");

        try {
            return modCandidateConstructor.newInstance(paths, null, -1L, metadata, true, new ArrayList<>());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to create ModCandidate", e);
        }
    }
}
