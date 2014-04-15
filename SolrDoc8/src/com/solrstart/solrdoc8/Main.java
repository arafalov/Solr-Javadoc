package com.solrstart.solrdoc8;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class Main {

    private static final PathConfig test47 = new PathConfig(
            "Solr 4.7.0",
            "/Volumes/RAMDisk/solr-4.7.0",
            new String[]{"/Volumes/RAMDisk/source-solr-4.7.0/lucene", "/Volumes/RAMDisk/source-solr-4.7.0/solr"});
    /**
     * Find all the classes present in Solr distribution (including war file)
     * Find which packages those classes come from
     * Find which source directories those packages match (not all will, some are just copied in)
     * Generate list of source directories and list of packages
     * (Advanced) Figure out a way to group those packages together (based on jars? or module directories?)
     * @param args
     */
    public static void main(String[] args) throws IOException{
        //assume test47 config
        PathConfig config = test47;
        Map<String, List<Path>> jars = findAllJars(config);
        Map<String, List<String>> jarPackageMap = getAllPackages(jars);

        Set<String> allPackages = new TreeSet<>();
        jarPackageMap.values().forEach(allPackages::addAll); //add all packages to the set

//        System.out.printf("\n\n\nAll Packages:\n");
//        allPackages.forEach(System.out::println);

        Map<Path, List<String>> usedSourceRootPaths = getUsedSourceRootPaths(config.sourceSearchRoots, allPackages);
        System.out.printf("Used source roots count %d\n", usedSourceRootPaths.size());


        Set<String> allValidPackages = new TreeSet<>();
        usedSourceRootPaths.values().forEach(allValidPackages::addAll);

        //roll-up, the packages to common, also needed, parent
        String mergedAllValidPackages = allValidPackages.stream().collect(Collectors.joining(":"));
        Set<String> rolledUpPackages = allValidPackages.stream()
                .filter(packageName ->
                                !allValidPackages
                                        .stream()
                                        .filter(superPackage -> packageName.contains(superPackage + '/'))
                                        .peek(superPackage -> System.out.printf("Matched super %s against %s\n", superPackage, packageName))
                                        .findAny().isPresent()
                ).collect(toSet());

        System.out.printf("Reduced package count from %d to %d\n", allValidPackages.size(), rolledUpPackages.size());


        System.out.println("Building group maps:");
        Set<String> packagesAlreadyGrouped = new TreeSet<>();

        //TODO: Broken, does not deal with duplicates well, assigns them to wrong categories
        LinkedList<String> jarNameSortedByPackageCount = new LinkedList<>();
        jarNameSortedByPackageCount.addAll(jarPackageMap.keySet());

        //Sort by count gives increasing numbers, then reverse to get the right order
        Collections.sort(jarNameSortedByPackageCount,Comparator.comparingInt(name -> jarPackageMap.get(name).size()));
        Collections.reverse(jarNameSortedByPackageCount);

        System.out.printf("Sanity checking groups. Before: %d, After: %d\n", jarPackageMap.size(), jarNameSortedByPackageCount.size());

        StringBuilder groupMaps = new StringBuilder();

        for(String jarName: jarNameSortedByPackageCount){
            if (jarName.indexOf("-test-framework") > 0 )
            {
                continue; //skip those pesky test-frameworks with duplicate package names for Mocks
            }
            System.out.printf("Package count for the jar %s is %d\n", jarName, jarPackageMap.get(jarName).size());
            List<String> allPackagesInJar = jarPackageMap.get(jarName);
            List<String> packages = allPackagesInJar.stream().filter(packageName -> allValidPackages.contains(packageName)).collect(toList());

            if (!packages.isEmpty()) {
                packages.stream().filter(name -> packagesAlreadyGrouped.contains(name)).forEach(name -> System.out.println("Skipping duplicate package: " + name));
                packages.removeAll(packagesAlreadyGrouped);
            }
            if (!packages.isEmpty()) {
                packagesAlreadyGrouped.addAll(packages);
            }

            if (packages.isEmpty())
            {
                System.out.println("Skipping the group for: " + jarName);
                continue;
            }

            if (packages.size() < allPackagesInJar.size())
            {
                System.out.println("Partial group for : " + jarName);
                System.out.println("Include:");
                packages.forEach(System.out::println);
                System.out.println("Exclude:");
                allPackagesInJar.removeAll(packages);
                allPackagesInJar.forEach(System.out::println);
            }
            else
            {
                System.out.println("Full group for : " + jarName);
            }


            groupMaps.append(String.format("-group %s %s ",
                jarName.replaceFirst("-(\\d+\\.)+jar", ""),
                packages.stream().collect(joining(":")).replace('/', '.')
            ));
        }

        System.out.print("\n\n======== Variables for the javadoc run ========\n\n");

        // Window title for this profile
        System.out.printf("export TITLE=\"%s\"\n", config.name);

        // Source Path value for javadoc
        System.out.printf("export SOURCE_PATH=%s\n",
                usedSourceRootPaths
                        .keySet().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(":")));

        // Packages for javadoc
        System.out.printf("export SOURCE_PACKAGES=%s\n",
                rolledUpPackages.stream()
                        .map(name -> name.replace('/', '.'))
                        .collect(Collectors.joining(":")));

        // Group maps for the overview page
        System.out.printf("export GROUP_MAPS=\"%s\"\n", groupMaps.toString().trim());

    }

    private static Map<String, List<String>> getAllPackages(Map<String, List<Path>> jars) throws IOException {
        Map<String, List<String>> jarPackageMap = new TreeMap<>();
        for(Map.Entry<String, List<Path>> entry: jars.entrySet())
        {
            String jarName = entry.getKey();
            Path firstPath = entry.getValue().get(0);

//            if (entry.getValue().size() == 1) {
//                System.out.printf("Jar %s found at %s\n", jarName, firstPath.toString());
//            }
//            else {
//                System.out.printf("\nJar %s found in multiple locations\n", jarName);
//                entry.getValue().forEach(path -> System.out.printf("  - %s\n", path));
//                System.out.println();
//            }

            List<String> packages = getPackages(firstPath);
            jarPackageMap.put(jarName, packages);
//            getPackages(firstPath).forEach(packageName -> System.out.printf("  %s\n", packageName));
        }
        return jarPackageMap;
    }

    private static Map<String, List<Path>> findAllJars(PathConfig config) throws IOException {
        FileSystem fileSystem = FileSystems.getDefault();

        Path startPath = fileSystem.getPath(config.jarSearchRoot);
        PathMatcher jarPathMatcher = fileSystem.getPathMatcher("glob:**/*.jar"); //assume war file got uncompressed on first run
        System.out.println("Start path: " + startPath.toString());

        return Files.walk(startPath)
//                .peek(path -> System.out.printf("Looking at path: %s, is jar?: %b\n", path.toString(), jarPathMatcher.matches(path)))
                .filter(jarPathMatcher::matches)
//                .peek(path -> System.out.println("Looking at filtered path: " + path.toString()))
                .collect(groupingBy(path -> path.getFileName().toString()));
    }

    public static List<String> getPackages(Path jarPath) throws IOException {
        LinkedList<String> jarPackages = new LinkedList<>();

        try (FileSystem zipFs = FileSystems.newFileSystem(jarPath, null)) {

            //find all .class files
            //some weird jars (jdom) have non-packaged names. skip those too.
            PathMatcher classPathMatcher = zipFs.getPathMatcher("glob:*/**/*.class");

            for (Path path: zipFs.getRootDirectories()) {
                Map<String, List<Path>> packagesMap = Files.walk(path)
                        .filter(classPathMatcher::matches)
                        .collect(groupingBy(classPath -> path.relativize(classPath).getParent().toString()));
//                packagesMap.keySet().forEach(packageName -> System.out.printf("Package: %s has %d classes\n", packageName, packagesMap.get(packageName).size()));
                jarPackages.addAll(packagesMap.keySet());
            }

            Collections.sort(jarPackages);
            return jarPackages; //TODO: What about duplicates if we really do have multiple roots?
        }
    }

    public static Map<Path, List<String>> getUsedSourceRootPaths(String[] sourceSearchRoots, Set<String> acceptedPackages) throws IOException {
        Map<Path, List<String>> usedModuleRoots = new TreeMap<>();

        for (String sourceRoot : sourceSearchRoots) {
            System.out.println("Looking at source root: " + sourceRoot);
            FileSystem fileSystem = FileSystems.getDefault();
            Path sourcePath = fileSystem.getPath(sourceRoot);
            PathMatcher moduleRootMatcher = fileSystem.getPathMatcher("glob:**/src/java");
            PathMatcher excludeTestMatcher = fileSystem.getPathMatcher("glob:**/test-framework/**");
            List<Path> moduleRootCandidates = Files.walk(sourcePath)
//                    .peek(path -> System.out.printf("Looking at source path %s\n", path.toString()))
                    .filter(moduleRootMatcher::matches)
                    .filter(path -> !excludeTestMatcher.matches(path))
//                    .peek(path -> System.out.printf("Found module candidate at %s\n", path.toString()))
                    .collect(toList());

            PathMatcher javaFileMatcher = fileSystem.getPathMatcher("glob:**/*.java");
            for (Path candidate : moduleRootCandidates) {
                List<String> validPackages = Files.walk(candidate)
                        .filter(javaFileMatcher::matches)
                        .map(path -> candidate.relativize(path.getParent()).toString())
                        .filter(pathString -> acceptedPackages.contains(pathString))
                        .sorted()
                        .distinct()
                        .collect(toList());

                System.out.printf("Candidate %s accepted: %b\n", candidate, !validPackages.isEmpty());

                if (!validPackages.isEmpty()) {
                    System.out.printf("Packages found: %d\n", validPackages.size());
                    validPackages.forEach(packageString -> System.out.printf("    %s\n", packageString));
                    usedModuleRoots.put(candidate, validPackages);
                }
            }
        }
        return usedModuleRoots;
    }
}