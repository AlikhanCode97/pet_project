package com.example.Games.gameHistory.dto;

public record FieldChange(
        String fieldName,
        String oldValue, 
        String newValue
) {

    public FieldChange {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        fieldName = fieldName.trim();
    }

    public static FieldChange of(String fieldName, String oldValue, String newValue) {
        return new FieldChange(fieldName, oldValue, newValue);
    }

    public boolean hasActualChange() {
        if (oldValue == null && newValue == null) {
            return false;
        }
        if (oldValue == null || newValue == null) {
            return true;
        }
        return !oldValue.equals(newValue);
    }

    public String getChangeDescription() {
        return String.format("Field '%s' changed from '%s' to '%s'", 
                           fieldName, 
                           oldValue != null ? oldValue : "null",
                           newValue != null ? newValue : "null");
    }
}
