import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;

public class Role {
    private final String id;
    private String name;
    private String description;
    private Set<Permission> permissions;
    public Role(String name, String description){
        this.id = "role_" + UUID.randomUUID().toString().substring(0, 8);
        this.name = name;
        this.description = description;
        this.permissions = new HashSet<>();
    }

    public void addPermission(Permission permission){
        if(permission != null)
            permissions.add(permission);
    }
    public void removePermission(Permission permission){
        if(!permissions.contains(permission)){
            throw new java.util.NoSuchElementException("This permisson does not exist in this role: " + name);
        }
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission){
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource){
        return permissions.stream()
                .anyMatch(p -> p.name().equalsIgnoreCase(permissionName)
                        && p.resource().equalsIgnoreCase(resource));
    }

    public Set<Permission> getPermissions(){
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Role{id='" + id + "', name='" + name + "'}";
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Role: %s [ID: %s]\n", name, id));
        sb.append(String.format("Description: %s\n", description));
        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        for (Permission p : permissions) {
            sb.append(" - ").append(p.format()).append("\n");
        }
        return sb.toString();
    }
}
