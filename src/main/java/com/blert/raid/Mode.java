package com.blert.raid;

import java.util.Optional;

public enum Mode {
    ENTRY,
    REGULAR,
    HARD;

    public static Optional<Mode> parse(String string) {
        switch (string.toLowerCase()) {
            case "entry":
            case "story":
                return Optional.of(ENTRY);
            case "normal":
            case "regular":
                return Optional.of(REGULAR);
            case "hard":
                return Optional.of(HARD);
            default:
                return Optional.empty();
        }
    }
}
