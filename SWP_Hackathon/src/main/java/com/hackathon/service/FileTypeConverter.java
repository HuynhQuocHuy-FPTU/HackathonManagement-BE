package com.hackathon.service;

import com.hackathon.entity.enums.FileType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                .map(FileType::valueOf)
                .collect(Collectors.toList());
    }
}
