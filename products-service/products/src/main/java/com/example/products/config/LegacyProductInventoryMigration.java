package com.example.products.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.example.products.models.Product;

@Component
public class LegacyProductInventoryMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LegacyProductInventoryMigration.class);

    private final MongoTemplate mongoTemplate;
    private final long defaultQuantity;

    public LegacyProductInventoryMigration(MongoTemplate mongoTemplate,
            @Value("${inventory.legacy-product-default-quantity:1}") long defaultQuantity) {
        this.mongoTemplate = mongoTemplate;
        this.defaultQuantity = defaultQuantity;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (defaultQuantity < 0) {
            throw new IllegalStateException("Legacy product default quantity cannot be negative");
        }

        Query legacyProducts = Query.query(Criteria.where("quantity").exists(false));
        var result = mongoTemplate.updateMulti(
                legacyProducts,
                new Update().set("quantity", defaultQuantity),
                Product.class);

        if (result.getModifiedCount() > 0) {
            log.info("Initialized quantity={} for {} legacy product(s)",
                    defaultQuantity, result.getModifiedCount());
        }
    }
}
