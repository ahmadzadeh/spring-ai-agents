package com.sirius.a2a_agents.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Spring AI tool that calls the Tavily Search API (https://docs.tavily.com/api-reference).
 * <p>
 * Usage: register via {@code .defaultTools(new TavilySearchTool(apiKey))} on a ChatClient.
 */
public class TavilySearchTool {

    private static final Logger log = LoggerFactory.getLogger(TavilySearchTool.class);
    private static final String TAVILY_SEARCH_URL = "https://api.tavily.com/search";

    private final RestClient restClient;

    public TavilySearchTool(String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(TAVILY_SEARCH_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Tool(description = "Search the web using Tavily. Returns titles, URLs, and content snippets for the query. Use for researching suppliers, companies, certifications, news, and public information.")
    public String search(
            @ToolParam(description = "The search query to run") String query,
            @ToolParam(description = "Maximum number of results to return (1-10)", required = false) Integer maxResults) {

        int limit = (maxResults != null && maxResults > 0 && maxResults <= 10) ? maxResults : 5;

        try {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "max_results", limit,
                    "search_depth", "advanced",
                    "include_answer", true
            );

            TavilySearchResponse response = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .body(TavilySearchResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return "No results found for query: " + query;
            }

            StringBuilder sb = new StringBuilder();

            if (response.answer() != null && !response.answer().isBlank()) {
                sb.append("Answer: ").append(response.answer()).append("\n\n");
            }

            sb.append("Results:\n");
            for (TavilySearchResult result : response.results()) {
                sb.append("- Title: ").append(result.title()).append("\n");
                sb.append("  URL: ").append(result.url()).append("\n");
                if (result.content() != null && !result.content().isBlank()) {
                    String snippet = result.content().length() > 500
                            ? result.content().substring(0, 500) + "..."
                            : result.content();
                    sb.append("  Snippet: ").append(snippet).append("\n");
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Tavily search error for query '{}': {}", query, e.getMessage());
            return "Search failed for query '" + query + "': " + e.getMessage();
        }
    }

    // -------------------------------------------------------------------------
    // Response records (matches Tavily API response shape)
    // -------------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TavilySearchResponse(
            String answer,
            List<TavilySearchResult> results
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TavilySearchResult(
            String title,
            String url,
            String content,
            @JsonProperty("raw_content") String rawContent,
            Double score
    ) {}
}
