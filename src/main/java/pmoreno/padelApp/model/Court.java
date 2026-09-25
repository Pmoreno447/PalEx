package pmoreno.padelApp.model;

import java.math.BigDecimal;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table (name = "courts")
public class Court {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    Long id;

    @Column (nullable = false)
    String name;

    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    // Es una primitiva no puede ser nulo no necesitamos anotación
    boolean active;

    @Column (nullable = false)
    int slotMinutes;

    @Column (nullable = false)
    LocalTime openTime;

    @Column (nullable = false)
    LocalTime closeTime;
    
    protected Court(){} // Exigido por JPA

    public Court(String name, BigDecimal price, boolean active, int slotMinutes, LocalTime openTime, LocalTime closTime){
        this.name = name;
        this.price = price;
        this.active = active;
        this.slotMinutes = slotMinutes;
        this.openTime = openTime;
        this.closeTime = closTime;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean getActive() {
        return active;
    }

    public void setSlotMinutes(int slotTime){
        this.slotMinutes = slotTime;
    }

    public int getSlotMinutes(){
        return this.slotMinutes;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }
}