package com.solrstart.javadoc.dependencies;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;
import com.sun.tools.javadoc.Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by arafalov on 4/20/14.
 */
public class Analyzer {

    private static BufferedWriter WRITER;

    public static boolean start(RootDoc root) throws IOException {
        Map<String, List<String>> packageMap = new TreeMap<>();

        for (ClassDoc classDoc : root.classes()) {
            String fromPackage = classDoc.containingPackage().name();
            List<String> packageRefs = packageMap.get(fromPackage);
            if (packageRefs == null) {
                packageRefs = new LinkedList<String>();
                packageMap.put(fromPackage, packageRefs);
            }
            Set<ClassDoc> relatedClasses = new TreeSet<>();

            addIfPresent(relatedClasses, classDoc.superclass());
            addIfPresent(relatedClasses, classDoc.containingClass());
            addIfPresent(relatedClasses, classDoc.interfaces());

            //TODO: This may not do what I think it does
            for (TypeVariable typeVariable : classDoc.typeParameters()) {
                for (Type bound : typeVariable.bounds()) {
                    System.out.print("Found bound parameter: " + bound.toString());
                    addIfPresent(relatedClasses, bound.asClassDoc());
                }
            }

//            boolean relationsFound = false;
            for (ClassDoc relatedClass : relatedClasses) {
                String toPackage = relatedClass.containingPackage().name();
                if (toPackage.startsWith("org.apache") && !toPackage.equals(fromPackage)) {
                    packageRefs.add(toPackage);
//                    WRITER.write(String.format("\"%s\" -> \"%s\";\n", fromName, toName));
//                    relationsFound = true;
                }
            };
//            if (!relationsFound) {
//                WRITER.write(String.format("\"%s\";\n", fromName)); //single node
//            }

        }

        //write out package links
        for (String fromPackageName : packageMap.keySet()) {
            List<String> toPackageList = packageMap.get(fromPackageName);
            if (toPackageList.isEmpty()) {
                WRITER.write(String.format("\"%s\";\n", fromPackageName)); //single node
                continue;
            }

            //get the packages and number of times they occur
            Map<String, Long> toPackageCounts = toPackageList.stream()
                    .collect(
                            Collectors.groupingBy(p -> p,
                                    Collectors.counting())
                    );

            for (String toPackageName : toPackageCounts.keySet()) {
                WRITER.write(String.format("\"%s\" -> \"%s\" [weight=\"%d\"]\n",
                        fromPackageName, toPackageName,
                        toPackageCounts.get(toPackageName)));
            }

        }

        return true;
    }

    private static void addIfPresent(Set<ClassDoc> set, ClassDoc... items)
    {
        if (items == null) { return; }

        for (ClassDoc item: items) {
            if (item != null) {
                set.add(item);
            }
        }
    }

    //this did not work too well
    private static void writeOutImportedClasses(ClassDoc classDoc, String fromName) throws IOException {
        ClassDoc[] importedClasses;

        try {
            importedClasses = classDoc.importedClasses();
        } catch (Exception ex) //actually InvocationTargetException, but it is somehow not declared
        {
            System.out.println("Some weird thing going on here: " + fromName);
            return;
        }

        boolean hasDependencies = false;
        if (importedClasses != null) {
            for (ClassDoc importedDoc : importedClasses) { //quick hack for test, using deprecated importedClasses
                String toName = importedDoc.qualifiedName();
                if (!toName.startsWith("org.apache.")) {
                    continue; //skip non-apache classes
                }

                WRITER.write(String.format("\"%s\" -> \"%s\";\n", fromName, toName));
                System.out.println(" Needs : " + toName);
                hasDependencies = true;
            }
        } else {
            System.out.println("NO dependencies found for: " + fromName);
        }

        if (!hasDependencies) {
            WRITER.write(String.format("\"%s\";\n", fromName)); //just the node itself. TODO: different shape maybe
        }
    }

    public static void main(String[] args) throws IOException {
        File outputFile = new File(args[0]);
        WRITER = new BufferedWriter(new FileWriter(outputFile), 10000);
        WRITER.write("digraph G {\n");

        Main.execute("JavadocIndexer", "com.solrstart.javadoc.dependencies.Analyzer", new String[]{
                "-sourcepath",
                "/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/common/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/icu/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/kuromoji/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/morfologik/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/phonetic/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/smartcn/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/stempel/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/analysis/uima/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/codecs/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/expressions/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/grouping/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/highlighter/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/join/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/memory/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/misc/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/queries/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/queryparser/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/spatial/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/suggest/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/analysis-extras/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/clustering/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/dataimporthandler-extras/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/dataimporthandler/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/extraction/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/langid/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/map-reduce/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/morphlines-cell/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/morphlines-core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/uima/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/contrib/velocity/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/solr/solrj/src/java",
//                "/Volumes/RAMDisk/source-solr-4.7.0/solr/core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/core/src/java",
//                "/Volumes/RAMDisk/source-solr-4.7.0/solr/core/src/java",
                "-subpackages",
                "org.apache"
//        org.tartarus.snowball:org.apache.lucene:org.apache.solr:org.egothor.stemmer
        });

        WRITER.write("}\n");
        WRITER.close();

        //Then run graphviz:
        // dot -Tsvg -O -Grankdir=LR dependencies.dot
    }

}
