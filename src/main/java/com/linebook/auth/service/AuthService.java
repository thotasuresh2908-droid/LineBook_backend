package com.linebook.auth.service;

import com.linebook.agent.dto.AgentResponse;
import com.linebook.agent.service.AgentService;
import com.linebook.auth.domain.AppUser;
import com.linebook.auth.domain.UserRole;
import com.linebook.auth.dto.ChangePasswordRequest;
import com.linebook.auth.dto.CreateUserRequest;
import com.linebook.auth.dto.LoginRequest;
import com.linebook.auth.dto.LoginResponse;
import com.linebook.auth.dto.UserResponse;
import com.linebook.auth.exception.ForbiddenException;
import com.linebook.auth.exception.InvalidUserException;
import com.linebook.auth.exception.UnauthorizedException;
import com.linebook.auth.exception.UserNotFoundException;
import com.linebook.auth.repository.AppUserRepository;
import com.linebook.auth.web.CurrentUser;
import com.linebook.auth.web.CurrentUserHolder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login accounts: sign in, "who am I", and the admin's add / deactivate / delete / reset-password
 * actions. An AGENT login is tied one-to-one to an agents row, so a collection recorded by that
 * person is always booked against that agent.
 */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final AgentService agents;
    private final PasswordHasher hasher;
    private final TokenService tokens;
    private final Clock clock;

    public AuthService(AppUserRepository users, AgentService agents, PasswordHasher hasher,
                       TokenService tokens, Clock clock) {
        this.users = users;
        this.agents = agents;
        this.hasher = hasher;
        this.tokens = tokens;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ sign in

    /** Checks the email and password against the database and returns a signed token. */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<AppUser> found = users.findByEmail(email);
        if (found.isEmpty()) {
            // Spend the same time as a real check, so a wrong email and a wrong password look alike.
            hasher.burnTime();
            throw new UnauthorizedException("Email or password is incorrect");
        }
        AppUser user = found.get();
        if (!hasher.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is incorrect");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("This login has been deactivated; ask your admin");
        }

        user.markLogin(LocalDateTime.now(clock));
        users.save(user);

        String token = tokens.issue(user.getId(), user.getRole().name(), user.getAgentId());
        return new LoginResponse(token, tokens.expiryOfNewToken(), UserResponse.from(user));
    }

    /** The signed-in person, re-read from the database so a deactivated login stops working at once. */
    @Transactional(readOnly = true)
    public UserResponse me() {
        CurrentUser caller = CurrentUserHolder.require();
        AppUser user = users.findById(caller.userId())
                .orElseThrow(() -> new UnauthorizedException("Your login no longer exists; please sign in again"));
        if (!user.isActive()) {
            throw new UnauthorizedException("This login has been deactivated; ask your admin");
        }
        return UserResponse.from(user);
    }

    /** Used by {@link com.linebook.auth.web.AuthFilter} to turn a token's claims into a caller. */
    @Transactional(readOnly = true)
    public Optional<CurrentUser> loadActive(String userId) {
        return users.findById(userId)
                .filter(AppUser::isActive)
                .map(u -> new CurrentUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.getAgentId()));
    }

    // ------------------------------------------------------------------ admin actions

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        CurrentUserHolder.requireAdmin();
        return users.findAllByOrderByEmailAsc().stream().map(UserResponse::from).toList();
    }

    /** Add a login. An AGENT login must point at an agent that does not have one yet. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        CurrentUserHolder.requireAdmin();
        return createInternal(request.email(), request.fullName(), request.password(),
                UserRole.from(request.role()), request.agentId());
    }

    /**
     * Same as {@link #create} but without the admin check; used by AgentService when "Add agent"
     * also creates the agent's login in the same transaction.
     */
    @Transactional
    public UserResponse createForAgent(String email, String fullName, String password, String agentId) {
        return createInternal(email, fullName, password, UserRole.AGENT, agentId);
    }

    private UserResponse createInternal(String rawEmail, String fullName, String password,
                                        UserRole role, String agentId) {
        String email = normalizeEmail(rawEmail);
        if (email.isBlank()) {
            throw new InvalidUserException("Email is required");
        }
        if (password == null || password.trim().length() < 6) {
            throw new InvalidUserException("Password must be at least 6 characters");
        }
        if (users.existsByEmail(email)) {
            throw new InvalidUserException("A login already exists for " + email);
        }

        String linkedAgent = null;
        if (role == UserRole.AGENT) {
            if (agentId == null || agentId.isBlank()) {
                throw new InvalidUserException("Choose the agent this login belongs to");
            }
            linkedAgent = agentId.trim();
            AgentResponse agent = agents.find(linkedAgent)
                    .orElseThrow(() -> new InvalidUserException("Unknown agent: " + agentId));
            if (users.findByAgentId(agent.id()).isPresent()) {
                throw new InvalidUserException(agent.name() + " already has a login");
            }
        }

        AppUser saved = users.save(new AppUser(
                nextId(),
                email,
                fullName == null || fullName.isBlank() ? email : fullName.trim(),
                hasher.hash(password.trim()),
                role,
                linkedAgent,
                true));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setActive(String id, boolean active) {
        CurrentUser caller = CurrentUserHolder.requireAdmin();
        AppUser user = findEntity(id);
        if (user.getId().equals(caller.userId()) && !active) {
            throw new InvalidUserException("You cannot deactivate your own login");
        }
        guardLastAdmin(user, active);
        user.changeActive(active);
        return UserResponse.from(users.save(user));
    }

    /**
     * Set a password. An admin can reset anyone's; anyone else can change only their own and must
     * give the current one.
     */
    @Transactional
    public void changePassword(String id, ChangePasswordRequest request) {
        CurrentUser caller = CurrentUserHolder.require();
        boolean ownAccount = caller.userId().equals(id);
        if (!ownAccount && !caller.isAdmin()) {
            throw new ForbiddenException("You can only change your own password");
        }
        AppUser user = findEntity(id);
        if (ownAccount && !caller.isAdmin()
                && !hasher.matches(request.currentPassword() == null ? "" : request.currentPassword(),
                        user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        user.changePasswordHash(hasher.hash(request.newPassword().trim()));
        users.save(user);
    }

    @Transactional
    public void delete(String id) {
        CurrentUser caller = CurrentUserHolder.requireAdmin();
        AppUser user = findEntity(id);
        if (user.getId().equals(caller.userId())) {
            throw new InvalidUserException("You cannot delete your own login");
        }
        guardLastAdmin(user, false);
        users.delete(user);
    }

    /** Removes an agent's login when the agent itself is deleted; no-op when there is none. */
    @Transactional
    public void deleteForAgent(String agentId) {
        users.findByAgentId(agentId).ifPresent(users::delete);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> findByAgentId(String agentId) {
        return users.findByAgentId(agentId).map(UserResponse::from);
    }

    // ------------------------------------------------------------------ helpers

    /** Keeps at least one usable admin login, so nobody can lock themselves out of the app. */
    private void guardLastAdmin(AppUser user, boolean stayingActive) {
        if (user.getRole() != UserRole.ADMIN || stayingActive) {
            return;
        }
        long activeAdmins = users.findAllByOrderByEmailAsc().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN && u.isActive())
                .count();
        if (activeAdmins <= 1) {
            throw new InvalidUserException("At least one active admin login must remain");
        }
    }

    private AppUser findEntity(String id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private String nextId() {
        return String.format("us-%04d", users.count() + 1);
    }

    private static String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
