package com.mishraachandan.util;

public final class HelperUtility {

    private HelperUtility() {} // prevents instantiation


    /**
     * Validates a password against the project's complexity policy.
     * <p>Rules: at least 8 characters, at most 128 characters, at least one
     * uppercase letter, at least one lowercase letter, at least one digit, and
     * at least one special (non-alphanumeric) character.
     *
     * @return an empty string if the password is valid, or a human-readable
     *         message describing what is wrong otherwise.
     */
    public static String getPasswordValidationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be blank";
        }
        StringBuilder message = new StringBuilder();

        if (password.length() < 8) {
            message.append("Password must be at least 8 characters long. ");
        }
        if (password.length() > 128) {
            message.append("Password must be at most 128 characters long. ");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            message.append("Password must contain at least one uppercase letter. ");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            message.append("Password must contain at least one lowercase letter. ");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            message.append("Password must contain at least one digit. ");
        }
        if (!password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            message.append("Password must contain at least one special character. ");
        }

        return message.toString().trim();
    }

}
