package crmapp.petsfort.JLogics.Models;

import java.io.Serializable;
import java.util.ArrayList; // Added import
import java.util.List;

public class Product implements Serializable {
    private String productId;
    private String productName;
    private String productDesc;
    private List<String> productImg;
    private String catId;
    private String catSub;
    private double costRate;
    private double costMrp;
    private double costGst;
    private double costDis;
    private int stock;
    private String id;


    public Product() {
        // Initialize fields to default non-null values to be safe
        this.productId = "";
        this.productName = "";
        this.productDesc = "";
        this.productImg = new ArrayList<>();
        this.catId = "";
        this.catSub = "";
        // Primitives have default values (0.0 for double, 0 for int)
        this.id = "";
    }

    // Copy constructor using setters to ensure null safety
    public Product(Product product) {
        if (product == null) {
            // Handle null input product, perhaps initialize with defaults
            this.productId = "";
            this.productName = "";
            this.productDesc = "";
            this.productImg = new ArrayList<>();
            this.catId = "";
            this.catSub = "";
            this.costRate = 0.0;
            this.costMrp = 0.0;
            this.costGst = 0.0;
            this.costDis = 0.0;
            this.stock = 0;
            this.id = "";
        } else {
            // Use setters for the new object, passing values from the source object.
            // Setters will handle potential nulls from the source object's fields.
            this.setProductId(product.productId);
            this.setProductName(product.productName);
            this.setProductDesc(product.productDesc);
            // Note: setProductImg performs a shallow copy by default if input is not null.
            // If a deep copy of the list is needed, modify the setProductImg method.
            this.setProductImg(product.productImg);
            this.setCatId(product.catId);
            this.setCatSub(product.catSub);
            this.setCostRate(product.costRate); // Primitives are copied directly
            this.setCostMrp(product.costMrp);
            this.setCostGst(product.costGst);
            this.setCostDis(product.costDis);
            this.setStock(product.stock);
            this.setId(product.id);
        }
    }

    // Getters (return default non-null if field is null)
    public String getProductId() {
        return (productId == null) ? "" : productId;
    }

    // Setters (check for null input and set default non-null)
    public void setProductId(String productId) {
        this.productId = (productId == null) ? "" : productId;
    }

    public String getProductName() {
        return (productName == null) ? "" : productName;
    }

    public void setProductName(String productName) {
        this.productName = (productName == null) ? "" : productName;
    }

    public String getProductDesc() {
        return (productDesc == null) ? "" : productDesc;
    }

    public void setProductDesc(String productDesc) {
        this.productDesc = (productDesc == null) ? "" : productDesc;
    }

    public List<String> getProductImg() {
        // Ensure the list is never null when accessed
        if (this.productImg == null) {
            this.productImg = new ArrayList<>();
        }
        return this.productImg;
    }

    public void setProductImg(List<String> productImg) {
        if (productImg == null) {
            this.productImg = new ArrayList<>(); // Set to new empty list if input is null
        } else {
            // Shallow copy: Assign the reference. Both original and copy point to the same list.
            // If you modify the list through one reference, the other sees the change.
            // For a deep copy (independent lists), use: this.productImg = new ArrayList<>(productImg);
            this.productImg = productImg;
        }
    }

    public String getCatId() {
        return (catId == null) ? "" : catId;
    }

    public void setCatId(String catId) {
        this.catId = (catId == null) ? "" : catId;
    }

    public String getCatSub() {
        return (catSub == null) ? "" : catSub;
    }

    public void setCatSub(String catSub) {
        this.catSub = (catSub == null) ? "" : catSub;
    }

    // Primitive getters/setters don't need null checks
    public double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public double getCostMrp() {
        return costMrp;
    }

    public void setCostMrp(double costMrp) {
        this.costMrp = costMrp;
    }

    public double getCostGst() {
        return costGst;
    }

    public void setCostGst(double costGst) {
        this.costGst = costGst;
    }

    public double getCostDis() {
        return costDis;
    }

    public void setCostDis(double costDis) {
        this.costDis = costDis;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getId() {
        return (id == null) ? "" : id;
    }

    public void setId(String id) {
        this.id = (id == null) ? "" : id;
    }
}