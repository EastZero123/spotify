package com.bd.spotify.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class FileHandlerUtil {

    @Value("${file.storage.song.path}")
    private String songStoragePath;

    @Value("${file.storage.image.path}")
    private String imageStoragePath;

    public String saveSongFileWithName(MultipartFile file, String customFilename) {
        return saveFileWithCustomName(file, songStoragePath, customFilename, "song");
    }

    public String saveImageFileWithName(MultipartFile file, String customFilename) {
        return saveFileWithCustomName(file, imageStoragePath, customFilename, "image");
    }

    private String saveFileWithCustomName(MultipartFile file, String storagePath, String customFilename, String fileType) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            Path directoryPath = Paths.get(storagePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            Path destinationPath = directoryPath.resolve(customFilename);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            return customFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + e.getMessage());
        }
    }

    public Resource loadSongFile(String filename) {
        return loadFile(filename, songStoragePath);
    }

    public Resource loadImageFile(String filename) {
        return loadFile(filename, imageStoragePath);
    }

    private Resource loadFile(String filename, String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if(resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to load file: " + e.getMessage());
        }
    }

    public void deleteSongFile(String filename) {
        deleteFile(filename, songStoragePath);
    }

    public void deleteImageFile(String filename) {
        deleteFile(filename, imageStoragePath);
    }

    private void deleteFile(String filename, String storagePath) {
        try {
            Path filePath = Paths.get(storagePath).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    public String extractFilename(String url) {
        if (url != null && url.contains("/")) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return null;
    }

    public String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
