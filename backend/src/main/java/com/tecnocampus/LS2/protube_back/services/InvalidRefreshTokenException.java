package com.tecnocampus.LS2.protube_back.services;

/**
 * Thrown when a refresh token is missing, invalid, expired or belongs to a user that no longer exists: the session
 * is over and the user has to log in again.
 */
public class InvalidRefreshTokenException extends LocalizedException {

    public InvalidRefreshTokenException() {
        super("error.session.expired", "Your session has expired, please log in again");
    }
}
