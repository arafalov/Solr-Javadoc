package com.solrstart.solrdoc8;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class Main {

    private static final PathConfig test47 = new PathConfig(
            "Solr 4.7.0",
            "/Volumes/RAMDisk/solr-4.7.0",
            new String[]{"/Volumes/RAMDisk/source-solr-4.7.0/lucene", "/Volumes/RAMDisk/source-solr-4.7.0/solr"},
            new String[]{"solr-core", "solr-.*", "lucene-core", "lucene-.*", ".*"});

    private static final PathConfig test48 = new PathConfig(
            "Solr 4.8.0",
            "/Volumes/RAMDisk/solr-4.8.0",
            new String[]{"/Volumes/RAMDisk/source-solr-4.8.0/lucene", "/Volumes/RAMDisk/source-solr-4.8.0/solr"},
            new String[]{"solr-core", "solr-.*", "lucene-core", "lucene-.*", ".*"});


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
        PathConfig config = test48;
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
//        String mergedAllValidPackages = allValidPackages.stream().collect(Collectors.joining(":"));
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

        // list of valid packages that is still left at the current stage of processing
        // required because some packages show up in multiple jars and cause:
        // java.util.MissingResourceException: Can't find resource for bundle com.sun.tools.doclets.internal.toolkit.resources.doclets, key doclet.Same_package_name_used

        Set<String> availableValidPackages = new TreeSet<String>();
        availableValidPackages.addAll(allValidPackages); //start with full set

        //copy jarMaps to group maps with final name convention
        Map<String, List<String>> groupMap = new TreeMap<>();
        jarPackageMap.keySet().stream()
                .filter(key -> key.indexOf("-test-framework") < 0) //skip test frameworks from the list
                .forEach(key -> groupMap.put(mapJarToGroupName(key), jarPackageMap.get(key)));

        StringBuilder groupMaps = new StringBuilder();
                
        for(String groupPattern: config.groupOrder)
        {
            List<String> matchingGroups =
                    groupMap.keySet().stream()
                    .filter(key -> Pattern.matches(groupPattern, key))
                    .collect(toList());
            Collections.sort(matchingGroups); //for now - just alphabetically
            for(String groupName: matchingGroups) {
                List<String> groupPackages = groupMap.get(groupName);
                List<String> validGroupPackages = groupPackages.stream()
                        .filter(packageName -> availableValidPackages.contains(packageName))
                        .collect(toList());
                if (!validGroupPackages.isEmpty()) {

                    //DEBUG
                    System.out.println("Adding group: " + groupName);

                    groupMaps.append(String.format("-group %s %s ",
                            groupName,
                            validGroupPackages.stream().collect(joining(":")).replace('/', '.')));

                    availableValidPackages.removeAll(validGroupPackages); //we assigned these package names; don't use again

                    //DEBUG
                    if (validGroupPackages.size() < groupPackages.size())
                    {
                        System.out.println("Partial group for : " + groupName);
                        System.out.println("Include:");
                        validGroupPackages.forEach(System.out::println);
                        System.out.println("Exclude:");
                        groupPackages.removeAll(validGroupPackages);
                        groupPackages.forEach(System.out::println);
                    }

                }
                else {
                    //DEBUG
//                    System.out.println("No valid packages found for group: " + groupName);
                }
                groupMap.remove(groupName); //to avoid matching later on more general regexps
            }
        }
        if (!groupMap.isEmpty())
        {
            System.err.println("Group order is not matching all packages. Leftover groups are:");
            groupMap.keySet().forEach(System.err::println);
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

    private static String mapJarToGroupName(String jarName) {
        String groupName =  jarName.replaceFirst("-(\\d+\\.)+jar", "");
        if (groupName.endsWith(".jar")){
            groupName = groupName.replace(".jar", "");
        }
        return groupName;
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