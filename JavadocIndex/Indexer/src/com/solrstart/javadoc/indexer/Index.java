package com.solrstart.javadoc.indexer;

import com.sun.javadoc.*;
import com.sun.tools.javadoc.Main;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class Index {

    /**
     * The name of the module that current run is for
     */
    private static String MODULE;

    /**
     * Sequence number. Global across multiple invocations of start() method, as long as we are in the same run
     */
    private static long ID_SEQ = 0; //global across all invocations

    /**
     * Global Solr Server instance.
     */
    private static SolrServer SOLR_SERVER;

    /**
     * Magic signature that gets invoked by the Javadoc as custom doclet
     * @param root
     * @return
     * @throws SolrServerException
     */
    public static boolean start(RootDoc root) throws SolrServerException, IOException {
        System.out.printf("Processing Module: %s, starting after ID: %d\n", MODULE, ID_SEQ);

        System.out.println("ClassDoc packages: " + root.classes().length);

        long id=0;

        Collection<SolrInputDocument> docList = new LinkedList<>();

        for (PackageDoc packageDoc : root.specifiedPackages()) {
            docList.clear();

            //Add the package itself
            String packageName = packageDoc.name();
            System.out.println("Package: " + packageName);
            SolrInputDocument packageInfo = createSolrDoc("package");
            packageInfo.addField("packageName", packageName, 10);
            packageInfo.addField("description", "Package ${packageName}");
            addComment(packageDoc, packageInfo);
            docList.add(packageInfo);

            //All types of classes (interfaces, errors, etc). Maybe split/later
            for (ClassDoc classDoc : packageDoc.allClasses()) {
                String className = classDoc.name();
                System.out.printf("    Class: %s\n", className);
                {
                    SolrInputDocument classInfo = createSolrDoc("class");
                    classInfo.addField("packageName", packageName); //no boost
                    classInfo.addField("className", className, 10);
                    classInfo.addField("description", "Class ${className} (in package ${packageName})");
                    addComment(classDoc, classInfo);

                    docList.add(classInfo);
                }

                //TODO: Rethink: a bit clunky. May become clearer after doing other reference types (e.g. @seeAlso)
                ClassDoc superDoc = classDoc.superclass();
                if (superDoc != null && !superDoc.qualifiedName().equals("java.lang.Object")) {
                    String superName = superDoc.name();
                    System.out.println("      Super: " + superName);
                    {
                        SolrInputDocument superInfo = createSolrDoc("inherit");
                        superInfo.addField("packageName", packageName); //no boost
                        superInfo.addField("className", superName); //no boost on super's name
                        superInfo.addField("sourceClassName", className);
                        superInfo.addField("description", "Class ${sourceClassName} (in package ${packageName}) inherits from Class ${className}");
                        addComment(classDoc, superInfo);
                        docList.add(superInfo);
                    }
                }

                for (MethodDoc methodDoc : classDoc.methods()) {
                    String methodName = methodDoc.name();
//                    System.out.printf("        Method: %s\n", methodName);
                    {
                        SolrInputDocument methodInfo = createSolrDoc("method");
                        methodInfo.addField("packageName", packageName); //no boost
                        methodInfo.addField("className", className);
                        methodInfo.addField("methodName", methodName, 10);
                        methodInfo.addField("description", "Method ${className}.${methodName} (in package ${packageName})");
                        addComment(methodDoc, methodInfo);
                        docList.add(methodInfo);
                    }
                }
            }
            SOLR_SERVER.add(docList);
        }
        SOLR_SERVER.commit();
        return true;

    }

    private static SolrInputDocument createSolrDoc(String type)
    {
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("module", MODULE);
        solrDoc.addField("id", ++ID_SEQ);
        solrDoc.addField("type", type);
        return solrDoc;

    }
    private static void addComment(Doc doc, SolrInputDocument solrInfo) {
        Tag[] commentTags = doc.firstSentenceTags();
        if (commentTags == null || commentTags.length == 0) {return;} //no comments to add

        solrInfo.addField("comment",
                Arrays.stream(commentTags)
                        .map(t -> t.text())
                        .collect(Collectors.joining())
        );
    }

    /***
     * This is a doclet driver that triggers Javadoc repeatedly with passed-in parameters
     * @param args - comes in groups of three: module sourcepath subpackages.
     *             sourcepath and subpackages both accept the list separated by colons (':')
     */
    public static void main(String[] args) throws IOException, SolrServerException {
        SOLR_SERVER = new HttpSolrServer( "http://localhost:8983/solr/JavadocCollection");

        SOLR_SERVER.deleteByQuery("*:*");
        SOLR_SERVER.commit();

        if (args.length % 3 != 0){
            System.err.println("We are expecting parameters in groups of 3: module sourcepath subpackages");
        }
        for(int offset = 0; offset<args.length; offset+=3){
            MODULE = args[offset]; //set for global access
            String sourcepath = args[offset+1];
            String subpackages = args[offset+2];

            Main.execute("JavadocIndexer", "com.solrstart.javadoc.indexer.Index", new String[]{
                    "-sourcepath", sourcepath,
                    "-subpackages", subpackages
            });
        }


//        Main.execute("JavadocIndexer", "com.solrstart.javadoc.indexer.Index", new String[]{
//                "-sourcepath",
//                "/Volumes/RAMDisk/source-solr-4.7.0/solr/core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/core/src/java",
//                "-subpackages",
//                "org.apache.solr"
//        });

        SOLR_SERVER.shutdown(); //just easier that tryin

    }

}
