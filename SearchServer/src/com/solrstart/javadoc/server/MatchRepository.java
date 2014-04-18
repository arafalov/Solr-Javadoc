package com.solrstart.javadoc.server;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * Created by arafalov on 4/18/14.
 */
public interface MatchRepository extends SolrCrudRepository<Match, String>{

    @Query(requestHandler = "/lookup", value = "?0")
    Page<Match> find(String query, Pageable page);
}
