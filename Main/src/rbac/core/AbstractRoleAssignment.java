package rbac.core;

import java.util.Objects;
import java.util.UUID;
package rbac.core;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    String assignmentId;
    User user;
    Role role;
    AssignmentMetadata metadata;

    public AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata){
        this.assignmentId = "asgn_" + UUID.randomUUID().toString().substring(0,8);
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.role = Objects.requireNonNull(role, "rbac.core.Role cannot be null");
        this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    }

    @Override public String assignmentId() { return assignmentId; }
    @Override public User user() { return user; }
    @Override public Role role() { return role; }
    @Override public AssignmentMetadata metadata() { return metadata; }

    @Override public abstract boolean isActive();
    @Override public abstract String assignmentType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractRoleAssignment that = (AbstractRoleAssignment) o;
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }

    public String summary() {
        return String.format("[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s",
                assignmentType(),
                role.getName(),
                user.username(),
                metadata.assignedBy(),
                metadata.assignedAt(),
                metadata.reason(),
                isActive() ? "ACTIVE" : "INACTIVE"
        );
    }
}
