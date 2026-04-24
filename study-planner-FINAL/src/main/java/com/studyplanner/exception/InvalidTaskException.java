package com.studyplanner.exception;

/*
 * InvalidTaskException - Thrown when a Task fails validation.
 * Stores the name of the field that caused the failure.
 */
public class InvalidTaskException extends Exception {

    private final String fieldName;

    public InvalidTaskException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    public InvalidTaskException(String message) {
        super(message);
        this.fieldName = "unknown";
    }

    // Returns the name of the field that failed validation.
    public String getFieldName() { return fieldName; }

    @Override
    public String toString() {
        return "InvalidTaskException[" + fieldName + "]: " + getMessage();
    }
}
