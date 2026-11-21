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

// Inscripción de un usuario en un torneo. Es una entidad y no un @ManyToMany porque la relación lleva datos propios.
// Sin restricción única (tournament_id, user_id): una inscripción vencida no debe impedir volver a inscribirse
@Entity
@Table (name = "tournament_registrations")
public class TournamentRegistration {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    // Igual que en Booking: es el id que se expone en la API y el que viaja a Stripe como client_reference_id
    @Column (nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "user_id", nullable = false)
    private User user;

    @Column (nullable = false)
    private LocalDateTime registeredAt;

    // Precio de inscripción en el momento de apuntarse, por si el del torneo cambia después
    @Column (nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Mismo ciclo que una reserva: pendiente hasta que se confirma el pago
    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private BookingState state;

    protected TournamentRegistration(){} // Exigido por JPA

    public TournamentRegistration(Tournament tournament, User user, LocalDateTime registeredAt,
                                  BigDecimal amount, BookingState state){
        this.tournament = tournament;
        this.user = user;
        this.registeredAt = registeredAt;
        this.amount = amount;
        this.state = state;
    }

    public Long getId(){
        return id;
    }

    public UUID getPublicId(){
        return publicId;
    }

    public Tournament getTournament(){
        return tournament;
    }

    public void setTournament(Tournament tournament){
        this.tournament = tournament;
    }

    public User getUser(){
        return user;
    }

    public void setUser(User user){
        this.user = user;
    }

    public LocalDateTime getRegisteredAt(){
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt){
        this.registeredAt = registeredAt;
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
}
