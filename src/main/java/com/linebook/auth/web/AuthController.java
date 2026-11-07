package com.linebook.auth.web;

import com.linebook.auth.dto.ChangePasswordRequest;
import com.linebook.auth.dto.CreateUserRequest;
import com.linebook.auth.dto.LoginRequest;
import com.linebook.auth.dto.LoginResponse;
import com.linebook.auth.dto.UpdateUserActiveRequest;
import com.linebook.auth.dto.UserResponse;
import com.linebook.auth.service.AuthService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Sign in and manage login accounts")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** The login page. Public: this is the only /api path that works without a token. */
    @Operation(summary = "Sign in with email and password")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    /** Called on every page load to confirm the saved token is still good. */
    @Operation(summary = "The signed-in user")
    @GetMapping("/me")
    public UserResponse me() {
        return service.me();
    }

    @Operation(summary = "List login accounts (admin)")
    @GetMapping("/users")
    public List<UserResponse> users() {
        return service.list();
    }

    @Operation(summary = "Add a login account (admin)")
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Activate or deactivate a login (admin)")
    @PatchMapping("/users/{id}/active")
    public UserResponse setActive(@PathVariable String id, @Valid @RequestBody UpdateUserActiveRequest request) {
        return service.setActive(id, request.active());
    }

    /** An admin resets any password; everyone else changes their own with the current one. */
    @Operation(summary = "Change or reset a password")
    @PatchMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable String id, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(id, request);
    }

    @Operation(summary = "Delete a login account (admin)")
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
