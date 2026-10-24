package com.linebook.customer.repository;

import com.linebook.customer.domain.ConnectionStatus;
import com.linebook.customer.domain.Customer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {

    long countByStatus(ConnectionStatus status);

    long countByStatusAndRecurring(ConnectionStatus status, boolean recurring);

    @Query("select distinct c.area from Customer c order by c.area")
    List<String> findDistinctAreas();

    @Query("""
            select new com.linebook.customer.repository.AgentCount(c.agentId, count(c))
            from Customer c
            where c.status = :status
            group by c.agentId
            """)
    List<AgentCount> countByAgent(@Param("status") ConnectionStatus status);
}
