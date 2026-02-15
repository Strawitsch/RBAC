public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description){
        if(name.isBlank()) throw new IllegalArgumentException("Name cannot be empty");
        if(resource.isBlank()) throw new IllegalArgumentException("resource cannot be empty");
        if(description.isBlank()) throw new IllegalArgumentException("description cannot be empty");

        if (name.contains(" ")) {
            throw new IllegalArgumentException("Permission name must not contain spaces");
        }

        this.name = name.toUpperCase();
        this.resource = resource.toLowerCase();
        this.description = description;
    }

    public String format(){
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        return this.name.contains(namePattern.toUpperCase()) &&
                this.resource.contains(resourcePattern.toLowerCase());
    }
}
