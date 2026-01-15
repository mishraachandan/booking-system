package com.mishraachandan.util;

public final class HelperUtility {

    private HelperUtility() {} // prevents instantiation

    
    public static String getPasswordValidationMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be blank";
        }
        StringBuilder message = new StringBuilder();

        if (password.length() < 8) {
            message.append("Password must be at least 8 characters long. ");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            message.append("Password must contain at least one uppercase letter. ");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            message.append("Password must contain at least one lowercase letter. ");
        }
        if (!password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            message.append("Password must contain at least one special character. ");
        }

        return message.toString().trim();
    }

}
