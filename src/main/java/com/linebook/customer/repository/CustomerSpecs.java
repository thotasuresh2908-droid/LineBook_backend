package com.linebook.customer.repository;

import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import jakarta.persistence.criteria.Expression;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Filters used by the Customers page: area, connection status and free-text search. */
public final class CustomerSpecs {

    private CustomerSpecs() {
    }

    public static Specification<Customer> inArea(String area) {
        return (root, query, cb) -> cb.equal(root.get("area"), area);
    }

    public static Specification<Customer> hasStatus(ConnectionStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Customer> idIn(Collection<String> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    /**
     * Same search as the UI: "{name} {accountNo} {stbNo} {vcNo} {phone}" contains the text,
     * case-insensitively.
     */
    public static Specification<Customer> matches(String text) {
        String pattern = "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> {
            Expression<String> haystack = root.get("name");
            for (String field : List.of("accountNo", "stbNo", "vcNo", "phone")) {
                haystack = cb.concat(cb.concat(haystack, " "), root.<String>get(field));
            }
            return cb.like(cb.lower(haystack), pattern, '\\');
        };
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
