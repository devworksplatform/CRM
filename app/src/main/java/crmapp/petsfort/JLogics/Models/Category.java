package crmapp.petsfort.JLogics.Models;

public class Category {
    private String id;
    private String name;
    private String image;

    public Category(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    // Setters (optional)
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "Category{id='" + id + "', name='" + name + "', image='" + image + "'}";
    }
}
