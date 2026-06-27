package com.example.products.search.events;

import com.example.products.search.ProductSearchDocument;

public record ProductSearchSyncEvent(
        String action,
        ProductSearchDocument product) {

    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";
}
