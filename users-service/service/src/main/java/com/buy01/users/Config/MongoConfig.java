package com.buy01.users.Config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${MONGODB_USERNAME}")
    private String username;

    @Value("${MONGODB_PWD}")
    private String password;

    @Value("${MONGODB_HOST}")
    private String host;

    @Value("${MONGODB_PORT}")
    private int port;

    @Value("${MONGO_DB}")
    private String database;

    @Value("${MONGODB_AUTH}")
    private String authenticationDatabase;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        String connectionString = String.format(
                "mongodb://%s:%s@%s:%d/%s?authSource=%s",
                username,
                password,
                host,
                port,
                database,
                authenticationDatabase);
        return MongoClients.create(connectionString);
    }
}