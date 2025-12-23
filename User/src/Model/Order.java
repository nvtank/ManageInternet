package Model;

import java.time.LocalDateTime;

public class Order {
    private String id;
    private String customer;
    private String customerName;
    private String food;
    private int quantity;
    private int total;
    private LocalDateTime time;

    public Order(String id, String customer, String customerName, String food, int quantity, int total, LocalDateTime time) {
        this.id = id;
        this.customer = customer;
        this.customerName = customerName;
        this.food = food;
        this.quantity = quantity;
        this.total = total;
        this.time = time;
    }

    public String getId() { return id; }
    public String getCustomer() { return customer; }
    public String getCustomerName() { return customerName; }
    public String getFood() { return food; }
    public int getQuantity() { return quantity; }
    public int getTotal() { return total; }
    public LocalDateTime getTime() { return time; }
}
