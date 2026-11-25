package com.tecnocampus.LS2.protube_back.controller.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * US-03: editable fields of the profile. All optional: a missing or blank field is cleared.
 */
public record UpdateProfileRequest(
        @Size(max = 500, message = "{validation.profile.description.size}") String description,
        @Size(max = 100, message = "{validation.profile.location.size}") String location,
        @Size(max = 200, message = "{validation.profile.website.size}")
        // Blank, or an http(s) address without spaces
        @Pattern(regexp = "^\\s*(https?://\\S+\\s*)?$", message = "{validation.profile.website.pattern}")
        String website) {
}
