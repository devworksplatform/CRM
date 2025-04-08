package shopline.com.JLogics.Models;

public class CartProduct extends Product {

    public CartProduct(Product product) {
        super(product);
    }
    public Long productCount;
    public Double actualCost;

}
