package com.buy01.users.Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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

    public void indexUser(User user) {
        if (user == null || user.id() == null) {
            return;
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
        } catch (RuntimeException | IOException e) {
            throw new IllegalStateException("Failed to index user " + user.id(), e);
        }
    }

    public void deleteUser(String userId) {
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}?refresh=wait_for", indexName, userId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Deleting an already-missing document is idempotent.
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to delete user from search index " + userId, e);
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
        List<User> users = userRepository.findAll();
        users.forEach(this::indexUser);
        return users.size();
    }

    private Map<String, Object> buildQuery(String query, String currentUserId) {
        List<Object> must = List.of(Map.of("multi_match", Map.of(
                "query", query,
                "fields", List.of("username^3", "email^2", "role"),
                "fuzziness", "AUTO")));
        List<Object> mustNot = currentUserId == null || currentUserId.isBlank()
                ? List.of()
                : List.of(Map.of("term", Map.of("id", currentUserId)));

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", must);
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
