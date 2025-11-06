package com.example.ollama.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Utility class for encoding images to base64 format
 */
public class ImageEncoder {

    private static final Set<String> SUPPORTED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp");

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * Encode image file to base64 string
     *
     * @param filePath Path to image file
     * @return Base64-encoded image string
     * @throws IOException If file cannot be read or is invalid
     */
    public static String encodeImageToBase64(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        // Validate file exists
        if (!Files.exists(path)) {
            throw new IOException("Image file not found: " + filePath);
        }

        // Validate file size
        long fileSize = Files.size(path);
        if (fileSize > MAX_FILE_SIZE) {
            throw new IOException("Image file too large: " + fileSize +
                    " bytes (max: " + MAX_FILE_SIZE + " bytes)");
        }

        // Validate file extension
        String extension = getFileExtension(filePath);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException("Unsupported image format: " + extension +
                    ". Supported: " + SUPPORTED_EXTENSIONS);
        }

        // Read and encode
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * Encode image to data URL format (for OpenAI-compatible APIs)
     *
     * @param filePath Path to image file
     * @return Data URL with base64-encoded image
     * @throws IOException If file cannot be read or is invalid
     */
    public static String encodeImageToDataUrl(String filePath) throws IOException {
        String base64 = encodeImageToBase64(filePath);
        String mimeType = getMimeType(filePath);
        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * Get MIME type from file extension
     */
    private static String getMimeType(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg"; // fallback
        };
    }

    /**
     * Get file extension
     */
    private static String getFileExtension(String filePath) {
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filePath.substring(lastDot + 1);
    }

    /**
     * Check if string is a URL
     */
    public static boolean isUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /**
     * Validate and process image paths
     * Returns base64-encoded strings for local files or URLs as-is
     *
     * @param imagePaths List of image file paths or URLs
     * @return List of base64-encoded images or URLs
     * @throws IOException If any file cannot be processed
     */
    public static List<String> processImagePaths(List<String> imagePaths) throws IOException {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return List.of();
        }

        List<String> processed = new ArrayList<>();
        for (String path : imagePaths) {
            if (isUrl(path)) {
                // URLs are passed as-is
                processed.add(path);
            } else {
                // Local files are encoded to base64
                processed.add(encodeImageToBase64(path));
            }
        }
        return processed;
    }

    /**
     * Process image paths and encode to data URLs for OpenAI-compatible APIs
     *
     * @param imagePaths List of image file paths or URLs
     * @return List of data URLs or original URLs
     * @throws IOException If any file cannot be processed
     */
    public static List<String> processImagePathsToDataUrls(List<String> imagePaths) throws IOException {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return List.of();
        }

        List<String> processed = new ArrayList<>();
        for (String path : imagePaths) {
            if (isUrl(path)) {
                // URLs are passed as-is
                processed.add(path);
            } else {
                // Local files are encoded to data URLs
                processed.add(encodeImageToDataUrl(path));
            }
        }
        return processed;
    }
}
