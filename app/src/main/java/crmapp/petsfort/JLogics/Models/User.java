package crmapp.petsfort.JLogics.Models;

public class User {
    public String uid, id;
    public final String name;
    public final String email;
    public final String role;
    public final String address;
    public double credits;
    public final int isBlocked;

    public User(String uid, String id, String name, String email, String role, String address, double credits, int isBlocked) {
        this.uid = uid;
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.address = address;
        this.credits = credits;
        this.isBlocked = isBlocked;
    }


    public static String resolveRoleToString(String role) {
        switch (role) {
            case "1":
                return "Client";
            case "2":
                return "Agent";
            case "3":
                return "Viewer";
            case "4":
                return "Admin";
            default:
                return "Unknown";
        }
    }
}
