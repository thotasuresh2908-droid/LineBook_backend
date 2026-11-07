package com.linebook.agent.web;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.dto.CreateAgentRequest;
import com.linebook.agent.dto.UpdateActiveRequest;
import com.linebook.agent.dto.UpdateAgentRequest;
import com.linebook.agent.service.AgentService;
import com.linebook.auth.web.CurrentUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    /** Admin only. Optionally creates the agent's login at the same time. */
    @Operation(summary = "Add an agent (admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse create(@Valid @RequestBody CreateAgentRequest request) {
        CurrentUserHolder.requireAdmin();
        return service.create(request);
    }

    @Operation(summary = "Edit an agent (admin)")
    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable String id, @Valid @RequestBody UpdateAgentRequest request) {
        CurrentUserHolder.requireAdmin();
        return service.update(id, request);
    }

    /** Deactivate (or reactivate) an agent. Inactive agents drop out of the dropdowns; their history is untouched. */
    @Operation(summary = "Deactivate or reactivate an agent (admin)")
    @PatchMapping("/{id}/active")
    public AgentResponse setActive(@PathVariable String id, @Valid @RequestBody UpdateActiveRequest request) {
        CurrentUserHolder.requireAdmin();
        return service.setActive(id, request.active());
    }

    /**
     * Permanently remove an agent and their login. Refused while they still have customers or
     * receipts, so nothing in the books is ever orphaned.
     */
    @Operation(summary = "Delete an agent (admin)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        CurrentUserHolder.requireAdmin();
        service.delete(id);
    }
}
