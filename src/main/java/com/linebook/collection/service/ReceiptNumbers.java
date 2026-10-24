package com.linebook.collection.service;

import java.time.LocalDate;

/** Hands out the day's receipt sequence numbers: 1, 2, 3 ... with no gaps or repeats, even under concurrency. */
public interface ReceiptNumbers {

    /** The next sequence number for receipts dated {@code day}. Must run inside the caller's transaction. */
    long next(LocalDate day);
}
