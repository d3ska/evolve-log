package com.deska.evolvelog.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

    /**
     * Stores the file and returns its relative path (e.g. "uploads/userId/uuid.jpg").
     * The path is persisted in the database and later passed back to load/delete.
     */
    String store(MultipartFile file, UUID userId);

    /**
     * Loads the file at the given relative path as a Spring Resource (for streaming).
     */
    Resource load(String relativePath);

    /**
     * Deletes the file at the given relative path. No-op if the file does not exist.
     */
    void delete(String relativePath);
}
