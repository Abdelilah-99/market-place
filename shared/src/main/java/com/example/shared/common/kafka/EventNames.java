package com.example.shared.common.kafka;

public final class EventNames {

    private EventNames() {
    }

    public static final String CREATE_USER_EVENT_NAME = "create-user-events";
    public static final String UPDATE_USER_EVENT_NAME = "update-user-events";
    public static final String REMOVE_USER_EVENT_NAME = "remove-user-events";

    public static final String CREATE_PRODUCT_EVENT_NAME = "create-product-events";
    public static final String UPDATE_PRODUCT_EVENT_NAME = "update-product-events";
    public static final String REMOVE_PRODUCT_EVENT_NAME = "remove-product-events";
    public static final String PRODUCT_SEARCH_SYNC_EVENT_NAME = "product-search-sync-events";
}
