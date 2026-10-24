package com.linebook.dashboard.web;

import com.linebook.dashboard.dto.AgentOverview;
import com.linebook.dashboard.service.AgentOverviewService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AgentOverviewController {

    private final AgentOverviewService service;

    public AgentOverviewController(AgentOverviewService service) {
        this.service = service;
    }

    /** The Agents page: one card of figures per agent. {@code cycle=yyyy-MM} and {@code date=yyyy-MM-dd} are optional. */
    @GetMapping("/overview")
    public List<AgentOverview> overview(
            @RequestParam(required = false) String cycle,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.overview(cycle, date, includeInactive);
    }
}
