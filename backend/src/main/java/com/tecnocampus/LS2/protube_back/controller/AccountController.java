package com.tecnocampus.LS2.protube_back.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.tecnocampus.LS2.protube_back.controller.dto.AuthResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.ChangeEmailRequest;
import com.tecnocampus.LS2.protube_back.controller.dto.ChangePasswordRequest;
import com.tecnocampus.LS2.protube_back.controller.dto.ChangeUsernameRequest;
import com.tecnocampus.LS2.protube_back.controller.dto.DeleteAccountRequest;
import com.tecnocampus.LS2.protube_back.controller.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.UpdateProfileRequest;
import com.tecnocampus.LS2.protube_back.services.AccountDeletionService;
import com.tecnocampus.LS2.protube_back.services.AccountService;
import com.tecnocampus.LS2.protube_back.services.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Account settings of the logged-in user (401 without a token, see SecurityConfig). Each change answers with a new
 * session (token and user) that the frontend keeps instead of the old one.
 */
@RestController
@Tag(name = "Account", description = "Settings of the logged-in user: username, email, password, profile, avatar and account deletion")
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AccountDeletionService accountDeletionService;
    private final ProfileService profileService;

    public AccountController(AccountService accountService, AccountDeletionService accountDeletionService,
                             ProfileService profileService) {
        this.accountService = accountService;
        this.accountDeletionService = accountDeletionService;
        this.profileService = profileService;
    }

    /**
     * US-03: saves the description, location and website of the profile (blank fields are cleared). 400 with the
     * field errors if a value is too long or the website is not an http(s) address.
     */
    @PutMapping("/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(profileService.updateProfile(authentication.getName(), request));
    }

    /**
     * US-03: replaces the avatar with a JPG, PNG or WebP image of at most 2 MB (400 otherwise).
     */
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateAvatar(@RequestPart("avatar") MultipartFile avatar,
                                                        Authentication authentication) {
        return ResponseEntity.ok(profileService.updateAvatar(authentication.getName(), avatar));
    }

    // US-03: removes the avatar; the profile shows the initial of the username again
    @DeleteMapping("/avatar")
    public ResponseEntity<ProfileResponse> deleteAvatar(Authentication authentication) {
        return ResponseEntity.ok(profileService.deleteAvatar(authentication.getName()));
    }

    // 409 if the username is taken
    @PutMapping("/username")
    public ResponseEntity<AuthResponse> changeUsername(@Valid @RequestBody ChangeUsernameRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(accountService.changeUsername(authentication.getName(), request));
    }

    // 409 if another account has the email
    @PutMapping("/email")
    public ResponseEntity<AuthResponse> changeEmail(@Valid @RequestBody ChangeEmailRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(accountService.changeEmail(authentication.getName(), request));
    }

    // 400 if the current password is missing or wrong
    @PutMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(accountService.changePassword(authentication.getName(), request));
    }

    /**
     * Deletes the account and everything that belongs to it (see AccountDeletionService). 400 if the password (or,
     * for Google accounts without one, the typed username) is missing or wrong. The frontend then logs out.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@RequestBody DeleteAccountRequest request, Authentication authentication) {
        accountDeletionService.deleteAccount(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
