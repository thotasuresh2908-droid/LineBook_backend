package com.linebook.billing.repository;

import com.linebook.billing.domain.BillPayment;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {

    boolean existsByCollectionId(String collectionId);

    /** Total paid on one bill; null when there are no payments. */
    @Query("select sum(p.amount) from BillPayment p where p.billId = :billId")
    BigDecimal sumByBillId(@Param("billId") String billId);

    /** Total paid per bill for every bill in a cycle (bills with no payments are absent). */
    @Query("""
            select new com.linebook.billing.repository.PaidTotal(p.billId, sum(p.amount))
            from BillPayment p
            where p.billId in (select b.id from Bill b where b.cycle = :cycle)
            group by p.billId
            """)
    List<PaidTotal> sumByCycle(@Param("cycle") String cycle);
}
