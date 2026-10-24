package com.linebook.agent.repository;

import com.linebook.agent.domain.Agent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findAllByOrderBySeqAsc();

    List<Agent> findByActiveTrueOrderBySeqAsc();
}
