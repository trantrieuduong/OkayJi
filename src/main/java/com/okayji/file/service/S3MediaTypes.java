package com.okayji.file.service;

import java.util.List;

public final class S3MediaTypes {

    public static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    public static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo",
            "video/webm", "video/x-matroska"
    );

    public static final List<String> ALLOWED_OTHER_FILE_TYPES = List.of(
            // Documents
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // Archives
            "application/zip", "application/x-rar-compressed",
            // Audio
            "audio/mpeg", "audio/wav", "audio/ogg"
    );

    public static boolean isAllowedFileType(String contentType) {
        return contentType != null && (ALLOWED_IMAGE_TYPES.contains(contentType)
                || ALLOWED_VIDEO_TYPES.contains(contentType)
                || ALLOWED_OTHER_FILE_TYPES.contains(contentType));
    }

    public static boolean isImageType(String contentType) {
        return contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType);
    }

    public static boolean isVideoType(String contentType) {
        return contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType);
    }
}
