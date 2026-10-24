package com.linebook.collection.web;

import com.linebook.collection.dto.CollectRequest;
import com.linebook.collection.dto.CollectResult;
import com.linebook.collection.dto.CollectionResponse;
import com.linebook.collection.dto.DailyTotal;
import com.linebook.collection.dto.DaySummary;
import com.linebook.collection.dto.ReceiptResponse;
import com.linebook.collection.service.CollectionQueryService;
import com.linebook.collection.service.CollectionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService service;
    private final CollectionQueryService queries;

    public CollectionController(CollectionService service, CollectionQueryService queries) {
        this.service = service;
        this.queries = queries;
    }

    /** "Save collection". 201 when recorded, 200 when the same collectionId had already been recorded. */
    @PostMapping
    public ResponseEntity<CollectionResponse> collect(@Valid @RequestBody CollectRequest request) {
        CollectResult result = service.collect(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.collection());
    }

    /** Collections between two dates (yyyy-MM-dd, inclusive; default today), newest first. */
    @GetMapping
    public List<CollectionResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String billId,
            @RequestParam(required = false) String customerId) {
        return queries.list(from, to, agentId, billId, customerId);
    }

    /** Today page: receipts, total collected and per-agent split for a day (default today). */
    @GetMapping("/summary")
    public DaySummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queries.summary(date);
    }

    /** Today page chart: collected per day for the last N days (default 14). */
    @GetMapping("/daily")
    public List<DailyTotal> daily(@RequestParam(required = false) Integer days) {
        return queries.daily(days);
    }

    @GetMapping("/{id}")
    public CollectionResponse get(@PathVariable String id) {
        return queries.get(id);
    }

    /** What the Receipt dialog prints. */
    @GetMapping("/{id}/receipt")
    public ReceiptResponse receipt(@PathVariable String id) {
        return queries.receipt(id);
    }
}
