package com.alpha.lanim.util;

public final class Validator {

    private Validator() {
    }

    public static String validateNickname(String s) {
        if (s == null || s.isBlank()) {
            return "Nickname cannot be empty";
        }
        if (s.length() > Constants.MAX_NICKNAME_LENGTH) {
            return "Nickname must be at most " + Constants.MAX_NICKNAME_LENGTH + " characters";
        }
        return null;
    }

    public static String validateRoomSecret(String s) {
        if (s == null || s.isBlank()) {
            return "Room secret cannot be empty";
        }
        return null;
    }

    public static String validateChatText(String s) {
        if (s == null || s.isBlank()) {
            return "Message cannot be empty";
        }
        if (s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > Constants.MAX_TEXT_LENGTH) {
            return "Message must be at most " + Constants.MAX_TEXT_LENGTH + " bytes (UTF-8)";
        }
        return null;
    }
}
