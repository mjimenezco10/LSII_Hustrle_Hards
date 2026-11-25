package com.tecnocampus.LS2.protube_back.services;

/**
 * US-03: no user and no channel of the video store has that name. Answered with 404.
 */
public class ProfileNotFoundException extends LocalizedException {

    public ProfileNotFoundException() {
        super("error.profile.notFound", "Profile not found");
    }
}
