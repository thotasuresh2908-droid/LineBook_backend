package com.linebook.agent.repository;

import com.linebook.agent.domain.Agent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findAllByOrderBySeqAsc();

    List<Agent> findByActiveTrueOrderBySeqAsc();

    /** Highest seq so far; used to number the next agent id even after some have been deleted. */
    @org.springframework.data.jpa.repository.Query("select coalesce(max(a.seq), 0) from Agent a")
    long maxSeq();
}
