package com.mrkirby153.kcuhc.discord.oauth;

/**
 * Exception thrown when an OAuth error occurs
 */
public class OAuthException extends RuntimeException {

    public OAuthException() {
        super();
    }

    public OAuthException(String message) {
        super(message);
    }

    public OAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
