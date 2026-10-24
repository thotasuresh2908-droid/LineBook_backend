package com.linebook.collection.exception;

public class CollectionNotFoundException extends RuntimeException {
    public CollectionNotFoundException(String id) {
        super("Collection not found: " + id);
    }
}
