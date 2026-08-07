package com.hackathon.entity.enums;

import java.util.Arrays;
import java.util.List;

public enum FileType {
    PDF("application/pdf", FileGroup.DOCUMENT),
    DOC("application/msword", FileGroup.DOCUMENT),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", FileGroup.DOCUMENT),
    PPT("application/vnd.ms-powerpoint", FileGroup.DOCUMENT),
    PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", FileGroup.DOCUMENT),
    XLS("application/vnd.ms-excel", FileGroup.DOCUMENT),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", FileGroup.DOCUMENT),
    ZIP(
            "application/zip",
            FileGroup.ARCHIVE,
            "application/x-zip-compressed",
            "application/x-zip",
            "multipart/x-zip"
    ),
    RAR(
            "application/x-rar-compressed",
            FileGroup.ARCHIVE,
            "application/vnd.rar",
            "application/rar",
            "application/x-rar",
            "application/octet-stream"
    ),
    PNG("image/png", FileGroup.IMAGE),
    JPG("image/jpeg", FileGroup.IMAGE),
    JPEG("image/jpeg", FileGroup.IMAGE),
    MP4("video/mp4", FileGroup.VIDEO);


    private final String mimeType;
    private final List<String> mimeTypes;
    private final FileGroup group;

    FileType(String mimeType, FileGroup group, String... alternativeMimeTypes) {
        this.mimeType = mimeType;
        this.mimeTypes = java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(mimeType),
                        Arrays.stream(alternativeMimeTypes)
                )
                .toList();
        this.group = group;
    }

    public String getMimeType() {
        return mimeType;
    }

    public FileGroup getGroup() {
        return group;
    }

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    public boolean isImage() {
        return group == FileGroup.IMAGE;
    }

    public static FileType fromExtension(String extension) {
        if (extension == null || extension.isBlank()) return null;
        String normalized = extension.trim().toLowerCase().replace(".", "");
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }

    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) return null;
        return Arrays.stream(values())
                .filter(type -> type.mimeTypes.stream()
                        .anyMatch(value -> value.equalsIgnoreCase(mimeType.trim())))
                .findFirst()
                .orElse(null);
    }

    public enum FileGroup {
        DOCUMENT, ARCHIVE, IMAGE, VIDEO
    }
}
