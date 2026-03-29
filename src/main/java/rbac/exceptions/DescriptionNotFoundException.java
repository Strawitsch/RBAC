package rbac.exceptions;

public class DescriptionNotFoundException extends RuntimeException {
    public DescriptionNotFoundException(String message) {
        super(message);
    }
}
