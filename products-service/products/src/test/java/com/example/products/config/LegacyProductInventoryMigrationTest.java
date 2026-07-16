package com.example.products.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.example.products.models.Product;
import com.mongodb.client.result.UpdateResult;

class LegacyProductInventoryMigrationTest {
    @Test
    void initializesOnlyLegacyProducts() throws Exception {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.updateMulti(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(UpdateResult.acknowledged(2, 2L, null));

        new LegacyProductInventoryMigration(mongoTemplate, 1)
                .run(new DefaultApplicationArguments());

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq(Product.class));
    }

    @Test
    void rejectsNegativeMigrationQuantity() {
        LegacyProductInventoryMigration migration =
                new LegacyProductInventoryMigration(mock(MongoTemplate.class), -1);

        assertThrows(IllegalStateException.class,
                () -> migration.run(new DefaultApplicationArguments()));
    }
}
