package dev.victorloures.ledger.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JobStatusConverter implements AttributeConverter<JobStatus, String> {

    @Override
    public String convertToDatabaseColumn(JobStatus status) {
        return status == null ? null : status.name().toLowerCase();
    }

    @Override
    public JobStatus convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : JobStatus.valueOf(dbValue.toUpperCase());
    }
}
