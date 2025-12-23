package Model;

import java.io.Serializable;

public class Food implements Serializable {
    private String name;
    private byte[] image;
    private int amount;

    public Food(String name, byte[] image, int amount) {
        this.name = name;
        this.image = image;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
