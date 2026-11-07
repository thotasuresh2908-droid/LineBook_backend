package com.linebook.agent.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateActiveRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.service.AgentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agents", description = "Collection agents")
@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService service;

    public AgentController(AgentService service) {
        this.service = service;
    }

    /** Agent dropdowns. Active agents by default; {@code includeInactive=true} for everyone. */
    @Operation(summary = "List agents (active by default)")
    @GetMapping
    public List<AgentResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @Operation(summary = "Get one agent")
    @GetMapping("/{id}")
    public AgentResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @Operation(summary = "Add an agent")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse create(@Valid @RequestBody CreateAgentRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Edit an agent")
    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable String id, @Valid @RequestBody UpdateAgentRequest request) {
        return service.update(id, request);
    }

    /** Deactivate (or reactivate) an agent. Inactive agents drop out of the dropdowns; their history is untouched. */
    @Operation(summary = "Deactivate or reactivate an agent")
    @PatchMapping("/{id}/active")
    public AgentResponse setActive(@PathVariable String id, @Valid @RequestBody UpdateActiveRequest request) {
        return service.setActive(id, request.active());
    }
}
