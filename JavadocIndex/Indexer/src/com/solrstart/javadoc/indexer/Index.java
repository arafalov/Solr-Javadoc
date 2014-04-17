package com.solrstart.javadoc.indexer;

import com.sun.javadoc.*;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;

public class Index {

    /**
     * Magic signature that gets invoked by the Javadoc as custom doclet
     * @param root
     * @return
     * @throws SolrServerException
     */
    public static boolean start(RootDoc root) throws SolrServerException, IOException {
        SolrServer server = new HttpSolrServer( "http://localhost:8983/solr/JavadocCollection");

        SolrInputDocument doc1 = new SolrInputDocument();
        doc1.addField( "id", 3);
        doc1.addField( "addr_from", "new from");
        doc1.addField( "addr_to", "new to" );
        doc1.addField( "subject", "new subject" );
        server.add(doc1);
        server.commit();


        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        QueryResponse rsp = server.query( query );
        SolrDocumentList docs = rsp.getResults();
        docs.forEach(doc -> {
            System.out.println("Document: ");
            doc.getFieldNames().forEach(field -> System.out.printf("  %s=%s\n", field, doc.get(field)));
        });

        server.shutdown();
        return true;

    }

    /***
     * This is a doclet. The main method here is just for testing purposes.
     * @param args
     */
    public static void main(String[] args) {
        try {
            start(null);
        } catch (SolrServerException|IOException e) {
            e.printStackTrace();
        }
    }

}
