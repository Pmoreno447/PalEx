package pmoreno.padelApp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    // Identificador que se expone en la API; el id secuencial queda solo para uso interno
    @Column (nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

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

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    protected Booking(){} // Exigido por JPA

    public Booking(LocalDateTime initDateTime, LocalDateTime endDateTime, BigDecimal amount,
                   BookingState state, Court court, User user){
        this.initDateTime = initDateTime;
        this.endDateTime = endDateTime;
        this.amount = amount;
        this.state = state;
        this.court = court;
        this.user = user;
    }

    public Long getId(){
        return id;
    }

    public UUID getPublicId(){
        return publicId;
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

    public User getUser(){
        return this.user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public Court getCourt(){
        return court;
    }

    public void setCourt(Court court){
        this.court = court;
    }
}
