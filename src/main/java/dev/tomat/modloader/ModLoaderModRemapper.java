package dev.tomat.modloader;


import dev.tomat.modloader.mappings.MinecraftBeta1_8_1_MappingProvider;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.tinyremapper.*;
import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ModLoaderModRemapper {
    private static final Pattern FILE_NAME_SANITIZING_PATTERN = Pattern.compile("[^\\w.\\-+]+");
    private static final HashMap<String, IMappingProvider> MAPPING_CONFIGS = new HashMap<>();

    static {
        MAPPING_CONFIGS.put("1.8.1", new MinecraftBeta1_8_1_MappingProvider());
    }

    public static void remap(Collection<ModCandidate> modCandidates, Path tmpDir, Path outputDir) {
        List<ModCandidate> modsToRemap = new ArrayList<>();

        for (ModCandidate mod : modCandidates) {
            if (mod.getRequiresRemap()) {
                modsToRemap.add(mod);
            }
        }

        if (modsToRemap.isEmpty()) return;

        HashMap<String, TinyRemapper> versionRemappers = new HashMap<>();

        Path[] classPath;
        try {
            classPath = getRemapClasspath().toArray(new Path[0]);
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to get remap classpath", e);
        }

        for (String version : MAPPING_CONFIGS.keySet()) {
            TinyRemapper remapper = TinyRemapper.newRemapper()
                    .withMappings(MAPPING_CONFIGS.get(version) /*TinyRemapperMappingsHelper.create(MAPPING_CONFIGS.get(version), "intermediary", launcher.getTargetNamespace())*/)
                    .renameInvalidLocals(false)
                    .build();
            versionRemappers.put(version, remapper);
            remapper.readClassPathAsync(classPath);
        }

        Map<ModCandidate, RemapInfo> infoMap = new HashMap<>();

        TinyRemapper remapper = null;
        try {
            for (ModCandidate mod : modsToRemap) {
                remapper = versionRemappers.get(mod.getVersion().getFriendlyString());
                RemapInfo info = new RemapInfo();
                infoMap.put(mod, info);

                InputTag tag = remapper.createInputTag();
                info.tag = tag;

                if (mod.hasPath()) {
                    List<Path> paths = mod.getPaths();
                    if (paths.size() != 1) throw new UnsupportedOperationException("multiple path for "+mod);

                    info.inputPath = paths.get(0);
                } else {
                    info.inputPath = mod.copyToDir(tmpDir, true);
                    info.inputIsTemp = true;
                }

                info.outputPath = outputDir.resolve(getDefaultFileName(mod));
                Files.deleteIfExists(info.outputPath);

                remapper.readInputsAsync(tag, info.inputPath);
            }

            //Done in a 2nd loop as we need to make sure all the inputs are present before remapping
            for (ModCandidate mod : modsToRemap) {
                remapper = versionRemappers.get(mod.getVersion().getFriendlyString());
                RemapInfo info = infoMap.get(mod);
                OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(info.outputPath).build();

                FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(info.inputPath, false);

                if (delegate.get() == null) {
                    throw new RuntimeException("Could not open JAR file " + info.inputPath.getFileName() + " for NIO reading!");
                }

                Path inputJar = delegate.get().getRootDirectories().iterator().next();
                outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);

                info.outputConsumerPath = outputConsumer;

                remapper.apply(outputConsumer, info.tag);
            }

            //Done in a 3rd loop as this can happen when the remapper is doing its thing.
            for (ModCandidate mod : modsToRemap) {
                remapper = versionRemappers.get(mod.getVersion().getFriendlyString());
                RemapInfo info = infoMap.get(mod);

                String accessWidener = mod.getMetadata().getAccessWidener();

                if (accessWidener != null) {
                    info.accessWidenerPath = accessWidener;

                    try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.inputPath, false)) {
                        FileSystem fs = jarFs.get();
                        info.accessWidener = remapAccessWidener(Files.readAllBytes(fs.getPath(accessWidener)), remapper.getEnvironment().getRemapper());
                    } catch (Throwable t) {
                        throw new RuntimeException("Error remapping access widener for mod '"+mod.getId()+"'!", t);
                    }
                }
            }

            remapper.finish();

            for (ModCandidate mod : modsToRemap) {
                remapper = versionRemappers.get(mod.getVersion().getFriendlyString());
                RemapInfo info = infoMap.get(mod);

                info.outputConsumerPath.close();

                if (info.accessWidenerPath != null) {
                    try (FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(info.outputPath, false)) {
                        FileSystem fs = jarFs.get();

                        Files.delete(fs.getPath(info.accessWidenerPath));
                        Files.write(fs.getPath(info.accessWidenerPath), info.accessWidener);
                    }
                }

                mod.setPaths(Collections.singletonList(info.outputPath));
            }
        } catch (Throwable t) {
            if (remapper != null)
                remapper.finish();

            for (RemapInfo info : infoMap.values()) {
                if (info.outputPath == null) {
                    continue;
                }

                try {
                    Files.deleteIfExists(info.outputPath);
                } catch (IOException e) {
                    Log.warn(LogCategory.MOD_REMAP, "Error deleting failed output jar %s", info.outputPath, e);
                }
            }

            throw new FormattedException("Failed to remap mods!", t);
        } finally {
            for (RemapInfo info : infoMap.values()) {
                try {
                    if (info.inputIsTemp) Files.deleteIfExists(info.inputPath);
                } catch (IOException e) {
                    Log.warn(LogCategory.MOD_REMAP, "Error deleting temporary input jar %s", info.inputIsTemp, e);
                }
            }
        }
    }

    private static byte[] remapAccessWidener(byte[] input, Remapper remapper) {
        AccessWidenerWriter writer = new AccessWidenerWriter();
        AccessWidenerRemapper remappingDecorator = new AccessWidenerRemapper(writer, remapper, "intermediary", "named");
        AccessWidenerReader accessWidenerReader = new AccessWidenerReader(remappingDecorator);
        accessWidenerReader.read(input, "intermediary");
        return writer.write();
    }

    private static List<Path> getRemapClasspath() throws IOException {
        String remapClasspathFile = System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE);

        if (remapClasspathFile == null) {
            throw new RuntimeException("No remapClasspathFile provided");
        }

        String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

        return Arrays.stream(content.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toList());
    }

    private static String getDefaultFileName(ModCandidate mod) {
        String ret = String.format("%s-%s-%s.jar", mod.getId(), FILE_NAME_SANITIZING_PATTERN.matcher(mod.getVersion().getFriendlyString()).replaceAll("_"), Long.toHexString(mixHash()));
        if (ret.length() > 64) {
            ret = ret.substring(0, 32).concat(ret.substring(ret.length() - 32));
        }

        return ret;
    }

    private static long mixHash() {
        long hash = -1;
        hash ^= hash >>> 33;
        hash *= -49064778989728563L;
        hash ^= hash >>> 33;
        hash *= -4265267296055464877L;
        hash ^= hash >>> 33;
        return hash;
    }

    private static class RemapInfo {
        InputTag tag;
        Path inputPath;
        Path outputPath;
        boolean inputIsTemp;
        OutputConsumerPath outputConsumerPath;
        String accessWidenerPath;
        byte[] accessWidener;
    }
}
