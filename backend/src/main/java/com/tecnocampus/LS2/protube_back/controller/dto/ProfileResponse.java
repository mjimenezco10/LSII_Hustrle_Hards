package com.tecnocampus.LS2.protube_back.controller.dto;

import com.tecnocampus.LS2.protube_back.domain.User;

import java.time.Instant;

/**
 * US-03: public profile of a user or of a channel of the video store. Only public data: never the email.
 * {@code registered} is false for channels of imported videos, which have no account and so no avatar, description,
 * location, website or join date. {@code avatarUrl} is a path under {@code /media}, or null without avatar.
 */
public record ProfileResponse(String username, boolean registered, String avatarUrl, String description,
                              String location, String website, Instant joinedAt, long videoCount) {

    public static ProfileResponse of(User user, long videoCount) {
        String avatarUrl = user.getAvatarFile() == null ? null : "/media/" + user.getAvatarFile();
        return new ProfileResponse(user.getUsername(), true, avatarUrl, user.getDescription(), user.getLocation(),
                user.getWebsite(), user.getCreatedAt(), videoCount);
    }

    public static ProfileResponse ofChannel(String channel, long videoCount) {
        return new ProfileResponse(channel, false, null, null, null, null, null, videoCount);
    }
}
