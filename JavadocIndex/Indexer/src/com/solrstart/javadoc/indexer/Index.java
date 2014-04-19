package com.solrstart.javadoc.indexer;

import com.sun.javadoc.*;
import org.apache.solr.client.solrj.*;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.*;
import com.sun.tools.javadoc.Main;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

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
                    docList.add(classInfo);
                }
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
                        docList.add(superInfo);
                    }
                }

                for (MethodDoc methodDoc : classDoc.methods()) {
                    String methodName = methodDoc.name();
                    System.out.printf("        Method: %s\n", methodName);
                    {
                        SolrInputDocument methodInfo = new SolrInputDocument();
                        methodInfo.addField("id", ++id);
                        methodInfo.addField("type", "methodName");
                        methodInfo.addField("packageName", packageName); //no boost
                        methodInfo.addField("className", className);
                        methodInfo.addField("methodName", methodName, 10);
                        methodInfo.addField("description", "Method ${className}.${methodName} (in package ${packageName})");
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
