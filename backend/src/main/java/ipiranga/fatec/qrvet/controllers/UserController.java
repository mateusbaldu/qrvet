package ipiranga.fatec.qrvet.controllers;

import ipiranga.fatec.qrvet.dtos.request.PaginationRequest;
import ipiranga.fatec.qrvet.dtos.request.ResetPasswordRequest;
import ipiranga.fatec.qrvet.dtos.request.UserRequest;
import ipiranga.fatec.qrvet.dtos.response.*;
import ipiranga.fatec.qrvet.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping({"/users", "/usuarios"})
    public PageResponse<UserResponse> list(
            @Valid @ModelAttribute PaginationRequest pagination) {
        return service.listAllUsers(pagination);
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createUser(r));
    }

    @PostMapping("/auth/confirm-invitation")
    public ResponseEntity<Void> confirmInvitation(@Valid @RequestBody ResetPasswordRequest request) {
        service.confirmUserInvitation(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/invitation")
    public ResponseEntity<Void> resendInvitation(@PathVariable Long id) {
        service.sendInvitationToUserById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/sessions")
    public PageResponse<ActiveSessionResponse> sessions(
            @PathVariable Long id,
            @Valid @ModelAttribute PaginationRequest pagination) {

        return service.listSessions(id, pagination);
    }

    @DeleteMapping("/users/{id}/sessions/{jti}")
    public ResponseEntity<Void> revokeSession(@PathVariable Long id, @PathVariable String jti) {
        service.revokeSession(id, jti);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/sessions")
    public ResponseEntity<Void> revokeAllSessions(@PathVariable Long id) {
        service.revokeAllSessions(id);

        return ResponseEntity.noContent().build();
    }
}
