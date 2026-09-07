package ipiranga.fatec.qrvet.api;

import ipiranga.fatec.qrvet.model.UserAccount;
import ipiranga.fatec.qrvet.security.AuthenticatedUser;
import ipiranga.fatec.qrvet.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
public class ProfileController {
    private final ProfileService profiles;

    public ProfileController(ProfileService profiles) { this.profiles = profiles; }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return UserResponse.from(profiles.user(principal.id()));
    }

    @PatchMapping("/me")
    public UserResponse update(@AuthenticationPrincipal AuthenticatedUser principal,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(profiles.update(principal.id(), request.name(), request.email(),
            request.phone(), request.crmv()));
    }

    @PatchMapping("/me/password")
    public MessageResponse password(@AuthenticationPrincipal AuthenticatedUser principal,
                                    @Valid @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(principal.id(), request.currentPassword(), request.newPassword());
        return new MessageResponse("Senha alterada com sucesso. Faça login novamente nos outros dispositivos.");
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse avatar(@AuthenticationPrincipal AuthenticatedUser principal,
                               @RequestParam("file") MultipartFile file) throws IOException {
        return UserResponse.from(profiles.updateAvatar(principal.id(), file.getBytes(), file.getContentType()));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> removeAvatar(@AuthenticationPrincipal AuthenticatedUser principal) {
        profiles.removeAvatar(principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> avatarContent(@PathVariable UUID id) {
        UserAccount user = profiles.user(id);
        if (user.getAvatarData() == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .contentType(MediaType.parseMediaType(user.getAvatarContentType()))
            .body(user.getAvatarData());
    }

    public record UpdateProfileRequest(@NotBlank @Size(max = 160) String name,
                                       @NotBlank @Email @Size(max = 190) String email,
                                       @Size(max = 30) String phone, @Size(max = 40) String crmv) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}
    public record MessageResponse(String message) {}
}
