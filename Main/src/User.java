import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    public static User validate(String username, String fullName, String email){
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("Full name cannot be empty");
            }
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email cannot be empty");
            }

            if (!USERNAME_PATTERN.matcher(username).matches()) {
                throw new IllegalArgumentException("Username must be 3-20 characters (letters, numbers, underscores)");
            }

            if (!email.contains("@") || !email.substring(email.indexOf("@")).contains(".")) {
                throw new IllegalArgumentException("Email must contain '@' and a dot after it");
            }

            return new User(username, fullName, email);
    }
    public String format() {
        return String.format("%s (%s) %s", username, fullName, email);
    }
}
