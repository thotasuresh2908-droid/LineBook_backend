package com.linebook.billing.repository;

import com.linebook.billing.domain.Bill;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, String> {

    List<Bill> findByCycleOrderByBillNoAsc(String cycle);

    Optional<Bill> findByCustomerIdAndCycle(String customerId, String cycle);

    /** Every bill of one customer, newest cycle first. */
    List<Bill> findByCustomerIdOrderByCycleDesc(String customerId);

    long countByCycle(String cycle);

    @Query("select b.customerId from Bill b where b.cycle = :cycle")
    Set<String> findCustomerIdsByCycle(@Param("cycle") String cycle);

    @Query("select distinct b.cycle from Bill b order by b.cycle desc")
    List<String> findDistinctCycles();

    /** Row lock so two concurrent collections can't both pass the "within pending balance" check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bill b where b.id = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") String id);
}
