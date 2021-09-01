package com.mrkirby153.kcuhc.discord.oauth;

/**
 * The list of states for access to the server
 */
public enum AccessState {
    GRANTED,
    DENIED_MISSING_OAUTH,
    DENIED_OAUTH_UNAUTHORIZED,
    DENIED_MISSING_SERVER,
    DENIED_UNKNOWN
}
