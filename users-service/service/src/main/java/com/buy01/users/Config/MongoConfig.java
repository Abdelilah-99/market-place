package com.buy01.users.Config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${MONGODB_USERNAME:admin}")
    private String username;

    @Value("${MONGODB_PWD:123123}")
    private String password;

    @Value("${MONGODB_HOST:localhost}")
    private String host;

    @Value("${MONGODB_PORT:27017}")
    private int port;

    @Value("${MONGO_DB:users_db}")
    private String database;

    @Value("${MONGODB_AUTH:admin}")
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