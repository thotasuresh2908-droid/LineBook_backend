package com.linebook.collection.dto;

/** {@code created} is false when the request was a replay of an already recorded collectionId. */
public record CollectResult(CollectionResponse collection, boolean created) {
}
