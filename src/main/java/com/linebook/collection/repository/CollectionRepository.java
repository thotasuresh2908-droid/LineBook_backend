package com.linebook.collection.repository;

import com.linebook.collection.domain.CollectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CollectionRepository
        extends JpaRepository<CollectionRecord, String>, JpaSpecificationExecutor<CollectionRecord> {

    /** Receipts already issued with this prefix (used to number the next receipt of the day). */
    long countByReceiptNoStartingWith(String prefix);
}
