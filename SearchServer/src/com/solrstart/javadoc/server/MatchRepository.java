package com.solrstart.javadoc.server;

import org.springframework.data.domain.Pageable;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.repository.Highlight;
import org.springframework.data.solr.repository.Query;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * Created by arafalov on 4/18/14.
 */
public interface MatchRepository extends SolrCrudRepository<Match, String>{

    @Query(requestHandler = "/lookup", value = "?0")
    @Highlight(prefix = "<strong>", postfix = "</strong>")
    HighlightPage<Match> find(String query, Pageable page);
}
