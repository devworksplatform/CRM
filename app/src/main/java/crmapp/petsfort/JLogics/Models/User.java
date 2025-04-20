package crmapp.petsfort.JLogics.Models;

public class User {
    public final String id, name, email, role, address;
    public double credits;
    public final int isBlocked;

    public User(String id, String name, String email, String role, String address, double credits, int isBlocked) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.address = address;
        this.credits = credits;
        this.isBlocked = isBlocked;
    }

    // Getters here
}
