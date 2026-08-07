package com.hackathon.service.submission;

import com.hackathon.entity.enums.FileType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Converter
public class FileTypeConverter implements AttributeConverter<List<FileType>, String> {
    @Override
    public String convertToDatabaseColumn(List<FileType> attribute) {
        if(attribute == null || attribute.isEmpty())
        return "";

        return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<FileType> convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isEmpty()) return new ArrayList<>();

        return Arrays.stream(dbData.split(","))
                .map(this::parseFileType)
                .collect(Collectors.toList());
    }

    // Đọc được cả tên enum chuẩn và dữ liệu cũ có khoảng trắng, dấu ngoặc hoặc MIME type.
    private FileType parseFileType(String value) {
        String normalized = value
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        try {
            return FileType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            FileType type = FileType.fromMimeType(normalized);
            if (type == null) {
                type = FileType.fromExtension(normalized);
            }
            if (type == null) {
                throw new IllegalArgumentException(
                        "Loại tệp không hợp lệ trong dữ liệu vòng thi: " + value,
                        exception
                );
            }
            return type;
        }
    }
}
