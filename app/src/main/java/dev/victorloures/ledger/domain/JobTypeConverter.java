package dev.victorloures.ledger.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JobTypeConverter implements AttributeConverter<JobType, String> {

    @Override
    public String convertToDatabaseColumn(JobType type) {
        return type == null ? null : type.name().toLowerCase();
    }

    @Override
    public JobType convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : JobType.valueOf(dbValue.toUpperCase());
    }
}
