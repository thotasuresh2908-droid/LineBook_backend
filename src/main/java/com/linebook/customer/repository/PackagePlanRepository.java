package com.linebook.customer.repository;

import com.linebook.customer.domain.PackagePlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackagePlanRepository extends JpaRepository<PackagePlan, String> {

    List<PackagePlan> findAllByOrderBySortOrderAsc();
}
