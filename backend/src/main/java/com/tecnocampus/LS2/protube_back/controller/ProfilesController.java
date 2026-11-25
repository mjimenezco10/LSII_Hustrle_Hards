package com.tecnocampus.LS2.protube_back.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.tecnocampus.LS2.protube_back.controller.dto.PageResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.ProfileResponse;
import com.tecnocampus.LS2.protube_back.controller.dto.VideoSummaryResponse;
import com.tecnocampus.LS2.protube_back.services.ProfileService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-03: public profiles, seen by anyone (see SecurityConfig). The owner edits theirs through /api/account/profile
 * and /api/account/avatar.
 */
@RestController
@Tag(name = "Profiles", description = "US-03: public profile of a user or channel and its videos")
@RequestMapping("/api/profiles")
public class ProfilesController {

    private final ProfileService profileService;

    public ProfilesController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Public profile of a user or of a channel of the video store. Answers 404 if neither exists.
     */
    @GetMapping("/{name}")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String name) {
        return ResponseEntity.ok(profileService.getProfile(name));
    }

    /**
     * Paginated videos of the profile, newest first. 404 if the profile does not exist, 400 for an invalid
     * {@code page} or {@code size}.
     */
    @GetMapping("/{name}/videos")
    public ResponseEntity<PageResponse<VideoSummaryResponse>> getProfileVideos(
            @PathVariable String name,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{validation.page.min}") int page,
            @RequestParam(defaultValue = "12")
            @Min(value = 1, message = "{validation.size.range}")
            @Max(value = 50, message = "{validation.size.range}") int size) {
        return ResponseEntity.ok(profileService.getProfileVideos(name, page, size));
    }
}
