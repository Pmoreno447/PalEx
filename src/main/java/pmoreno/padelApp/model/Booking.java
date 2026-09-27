package pmoreno.padelApp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table (name = "bookings")
public class Booking {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private LocalDateTime initDateTime;

    @Column (nullable = false)
    private LocalDateTime endDateTime;

    @Column (nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private BookingState state;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "court_id", nullable = false)
    private Court court;

    protected Booking(){} // Exigido por JPA

    public Long getId(){
        return id;
    }

    public LocalDateTime getInitDateTime(){
        return initDateTime;
    }

    public void setInitDateTime(LocalDateTime initDateTime){
        this.initDateTime = initDateTime;
    }

    public LocalDateTime getEndDateTime(){
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime){
        this.endDateTime = endDateTime;
    }

    public BigDecimal getAmount(){
        return amount;
    }

    public void setAmount(BigDecimal amount){
        this.amount = amount;
    }

    public BookingState getState(){
        return state;
    }

    public void setState(BookingState state){
        this.state = state;
    }

    public Court getCourt(){
        return court;
    }

    public void setCourt(Court court){
        this.court = court;
    }
}
