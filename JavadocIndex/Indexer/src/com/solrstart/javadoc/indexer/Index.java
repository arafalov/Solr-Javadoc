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
     * Magic signature that gets invoked by the Javadoc as custom doclet
     * @param root
     * @return
     * @throws SolrServerException
     */
    public static boolean start(RootDoc root) throws SolrServerException, IOException {
        SolrServer server = new HttpSolrServer( "http://localhost:8983/solr/JavadocCollection");

        System.out.println("ClassDoc packages: " + root.classes().length);

        server.deleteByQuery("*:*");
        server.commit();

        long id=0;

        Collection<SolrInputDocument> docList = new LinkedList<>();

        for (PackageDoc packageDoc : root.specifiedPackages()) {
            docList.clear();

            //Add the package itself
            String packageName = packageDoc.name();
            System.out.println("Package: " + packageName);
            SolrInputDocument packageInfo = new SolrInputDocument();
            packageInfo.addField("id", ++id);
            packageInfo.addField("type", "package");
            packageInfo.addField("packageName", packageName, 10);
            packageInfo.addField("description", "Package ${packageName}");
            addComment(packageDoc, packageInfo);
            docList.add(packageInfo);

            //All types of classes (interfaces, errors, etc). Maybe split/later
            for (ClassDoc classDoc : packageDoc.allClasses()) {
                String className = classDoc.name();
                System.out.printf("    Class: %s\n", className);
                {
                    SolrInputDocument classInfo = new SolrInputDocument();
                    classInfo.addField("id", ++id);
                    classInfo.addField("type", "class");
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
                        SolrInputDocument superInfo = new SolrInputDocument();
                        superInfo.addField("id", ++id);
                        superInfo.addField("type", "inherit");
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
                    System.out.printf("        Method: %s\n", methodName);
                    {
                        SolrInputDocument methodInfo = new SolrInputDocument();
                        methodInfo.addField("id", ++id);
                        methodInfo.addField("type", "method");
                        methodInfo.addField("packageName", packageName); //no boost
                        methodInfo.addField("className", className);
                        methodInfo.addField("methodName", methodName, 10);
                        methodInfo.addField("description", "Method ${className}.${methodName} (in package ${packageName})");
                        addComment(methodDoc, methodInfo);
                        docList.add(methodInfo);
                    }
                }
            }
            server.add(docList);
        }
        server.commit();
        server.shutdown();
        return true;

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
     * This is a doclet. The main method here is just for testing purposes.
     * @param args
     */
    public static void main(String[] args) {
        Main.execute("JavadocIndexer", "com.solrstart.javadoc.indexer.Index", new String[]{
                "-sourcepath",
                "/Volumes/RAMDisk/source-solr-4.7.0/solr/core/src/java:/Volumes/RAMDisk/source-solr-4.7.0/lucene/core/src/java",
                "-subpackages",
                "org.apache.solr"
        });
    }

}
