package com.deska.evolvelog.storage;

import com.deska.evolvelog.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    private final Path uploadRoot;

    public LocalStorageService(@Value("${app.storage.local.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, UUID userId) {
        String extension = getExtension(file.getOriginalFilename());
        Path userDir = uploadRoot.resolve(userId.toString());
        Path destination = userDir.resolve(UUID.randomUUID() + "." + extension);

        try {
            Files.createDirectories(userDir);
            file.transferTo(destination);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        return uploadRoot.relativize(destination).toString().replace("\\", "/");
    }

    @Override
    public Resource load(String relativePath) {
        Path filePath = uploadRoot.resolve(relativePath).normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        return new FileSystemResource(filePath);
    }

    @Override
    public void delete(String relativePath) {
        Path filePath = uploadRoot.resolve(relativePath).normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file path");
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
