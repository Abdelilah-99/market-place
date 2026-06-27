package com.example.media.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;

import java.io.File;

@Configuration
@EnableFilesystemStores
public class ContentConfig {

    @Value("${spring.content.fs.filesystem-root:/storage}")
    private String storageRoot;

    @Bean
    public File filesystemRoot() {
        File dir = new File(storageRoot);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Failed to create media storage directory: " + dir.getAbsolutePath());
        }
        System.out.println("Spring Content directory: " + dir.getAbsolutePath());
        return dir;
    }

    @Bean
    public FileSystemResourceLoader fileSystemResourceLoader() {
        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
    }

}
