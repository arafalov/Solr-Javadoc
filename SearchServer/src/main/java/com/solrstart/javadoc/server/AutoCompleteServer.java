package com.solrstart.javadoc.server;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightPage;
import org.springframework.data.solr.repository.config.EnableSolrRepositories;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@EnableAutoConfiguration
@EnableSolrRepositories("com.solrstart.javadoc.server")
public class AutoCompleteServer {

    @Autowired
    private MatchRepository matchRepository;

    // Base URL (with final /) of where Javadoc lives. use --baseURL=/xyz to pass it in or via properties file
    @Value("${baseURL}")
    private String baseURL;


//    @RequestMapping("/lookup")
    @RequestMapping("${lookupURL}")
    @ResponseBody
    List<Match> home(
            @RequestParam(value="query", required=false, defaultValue="solr") String query
    ){

        List<Match> results = new LinkedList<>();

        //remove any characters that are not alphanumerics, spaces, underscores, dollars, or fullstops
        query = query.replaceAll("[^a-zA-Z0-9 _$.]", " ");
        query = query.replaceAll(" +", " "); //replace multiple spaces with one
        if (query.length()<1) {
            return null;
        }

        HighlightPage<Match> searchResults = matchRepository.find(query, new PageRequest(0, 10));
        Map<String, String> overrides = new TreeMap<>();
        for (HighlightEntry<Match> hightlightEntity : searchResults.getHighlighted()) {
            Match match = hightlightEntity.getEntity();
            overrides.clear();
            hightlightEntity.getHighlights().forEach(hl -> overrides.put(hl.getField().getName(), hl.getSnipplets().get(0))); //ignore multiple snippets for now
            match.createHTMLDescription(overrides);
//            match.lockTheURL("http://localhost:8983/javadoc");
//            match.lockTheURL("/javadoc");
            match.lockTheURL(baseURL);
            results.add(match);
        }

        return results;
    }


    @Bean
    public SolrServer solrServer() {
        return new HttpSolrServer("http://localhost:8983/solr/JavadocCollection");
    }

    @Bean
    public SolrTemplate solrTemplate(SolrServer server) throws Exception {
        return new SolrTemplate(server);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AutoCompleteServer.class, args);
    }

}
