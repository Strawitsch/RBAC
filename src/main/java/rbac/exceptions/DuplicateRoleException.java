package rbac.exceptions;

public class DuplicateRoleException extends RuntimeException {
    public DuplicateRoleException(String message) {
        super(message);
    }
}