package com.buy01.users.Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class UserSearchService {
    private static final Logger log = LoggerFactory.getLogger(UserSearchService.class);
    private static final int REINDEX_BATCH_SIZE = 100;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final String indexName;
    private volatile boolean indexReady = false;

    public UserSearchService(
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Value("${search.opensearch.host}") String host,
            @Value("${search.opensearch.port}") int port,
            @Value("${search.opensearch.scheme}") String scheme,
            @Value("${search.users.index}") String indexName) {
        this.restClient = RestClient.builder()
                .baseUrl(scheme + "://" + host + ":" + port)
                .build();
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.indexName = indexName;
    }

    @PostConstruct
    public void initializeSearchIndex() {
        try {
            ensureIndexExists();
        } catch (RuntimeException | IOException ignored) {
            indexReady = false;
        }
    }

    public boolean indexUser(User user) {
        if (user == null || user.id() == null) {
            return false;
        }

        try {
            ensureIndexExists();
            UserSearchDocument document = UserSearchDocument.fromUser(user);
            restClient.put()
                    .uri("/{index}/_doc/{id}?refresh=wait_for", indexName, document.id())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(document)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException | IOException e) {
            indexReady = false;
            log.warn("Failed to index user {}. Profile data was saved, but search may be stale until reindex runs.",
                    user.id(), e);
            return false;
        }
    }

    public boolean deleteUser(String userId) {
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}?refresh=wait_for", indexName, userId)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ignored) {
            // Deleting an already-missing document is idempotent.
            return true;
        } catch (RuntimeException e) {
            log.warn("Failed to delete user {} from search index. The database delete still completed.", userId, e);
            return false;
        }
    }

    public PageResponseDTOs<ProfileResDTOs> search(String query, String currentUserId, int page, int size) {
        String safeQuery = query == null ? "" : query.trim();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);

        if (safeQuery.length() < 2) {
            return new PageResponseDTOs<>(List.of(), 0, safePage, safeSize, 0, false);
        }

        try {
            ensureIndexExists();

            Map<String, Object> request = new LinkedHashMap<>();
            request.put("from", safePage * safeSize);
            request.put("size", safeSize);
            request.put("query", buildQuery(safeQuery, currentUserId));

            JsonNode response = restClient.post()
                    .uri("/{index}/_search", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode hits = response.path("hits");
            List<ProfileResDTOs> items = new ArrayList<>();
            for (JsonNode hit : hits.path("hits")) {
                UserSearchDocument document = objectMapper.treeToValue(hit.path("_source"), UserSearchDocument.class);
                items.add(toProfile(document));
            }

            long total = hits.path("total").path("value").asLong(items.size());
            int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
            return new PageResponseDTOs<>(items, total, safePage, safeSize, totalPages, safePage + 1 < totalPages);
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException("Failed to search users", e);
        }
    }

    public long reindexAllUsers() {
        long indexed = 0;
        int page = 0;
        Page<User> users;

        do {
            users = userRepository.findAll(PageRequest.of(page, REINDEX_BATCH_SIZE));
            indexed += users.getContent().stream()
                    .filter(this::indexUser)
                    .count();
            page++;
        } while (users.hasNext());

        return indexed;
    }

    private Map<String, Object> buildQuery(String query, String currentUserId) {
        List<Object> should = List.of(
                Map.of("match_bool_prefix", Map.of("username", Map.of(
                        "query", query,
                        "boost", 3))),
                Map.of("match_bool_prefix", Map.of("email", Map.of(
                        "query", query,
                        "boost", 2))));
        List<Object> mustNot = currentUserId == null || currentUserId.isBlank()
                ? List.of()
                : List.of(Map.of("term", Map.of("id", currentUserId)));

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("should", should);
        bool.put("minimum_should_match", 1);
        bool.put("must_not", mustNot);
        return Map.of("bool", bool);
    }

    private void ensureIndexExists() throws IOException {
        if (indexReady) {
            return;
        }

        if (indexExists()) {
            indexReady = true;
            return;
        }

        restClient.put()
                .uri("/{index}", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "settings": {
                            "analysis": {
                              "analyzer": {
                                "user_text": {
                                  "type": "standard"
                                }
                              }
                            }
                          },
                          "mappings": {
                            "properties": {
                              "id": { "type": "keyword" },
                              "username": { "type": "text", "analyzer": "user_text", "fields": { "keyword": { "type": "keyword" } } },
                              "email": { "type": "text", "analyzer": "user_text", "fields": { "keyword": { "type": "keyword" } } },
                              "role": { "type": "keyword" },
                              "avatarUrl": { "type": "keyword" }
                            }
                          }
                        }
                        """)
                .retrieve()
                .toBodilessEntity();
        indexReady = true;
    }

    private boolean indexExists() {
        try {
            restClient.head()
                    .uri("/{index}", indexName)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }

    private ProfileResDTOs toProfile(UserSearchDocument document) {
        return new ProfileResDTOs(
                document.id(),
                document.username(),
                document.email(),
                document.role(),
                document.avatarUrl());
    }
}
