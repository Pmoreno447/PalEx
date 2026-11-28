package pmoreno.padelApp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;


@Entity 
public class Tournament {
    @Id
    @GeneratedValue  (strategy = GenerationType.IDENTITY)
    private long id;

    // Identificador que se expone en la API; el id secuencial queda solo para uso interno
    @Column (nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column (nullable = false)
    private String name;

    // Por defecto la columna sería VARCHAR(255): se amplía al límite que admite el DTO
    @Column (nullable = false, length = 10000)
    private String description;

    @Column (nullable = false)
    private LocalDateTime startDate;

    @Column (nullable = false)
    private LocalDateTime endDate;

    @Column (nullable = false, precision = 10, scale = 2)
    private BigDecimal entryPrice;

    private Integer maxSlots;

    // Plazas ocupadas: todo torneo empieza sin inscritos
    @Column (nullable = false)
    private Integer actualSlots = 0;

    // Todo torneo nace abierto a inscripciones
    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private TournamentState state = TournamentState.OPEN;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "tournament_courts",
            joinColumns = @JoinColumn(name = "tournament_id"),
            inverseJoinColumns = @JoinColumn(name = "court_id"))
    private Set<Court> courts = new HashSet<>();

    protected Tournament(){} // Exigido por JPA

    public Tournament(String name, String description, LocalDateTime startDate, LocalDateTime endDate,
                      BigDecimal entryPrice, Integer maxSlots, Set<Court> courts){
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.entryPrice = entryPrice;
        this.maxSlots = maxSlots;
        this.courts = courts;
    }

    public long getId(){
        return id;
    }

    public UUID getPublicId(){
        return publicId;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public LocalDateTime getStartDate(){
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate){
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate(){
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate){
        this.endDate = endDate;
    }

    public BigDecimal getEntryPrice(){
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice){
        this.entryPrice = entryPrice;
    }

    public Integer getMaxSlots(){
        return maxSlots;
    }

    public void setMaxSlots(Integer maxSlots){
        this.maxSlots = maxSlots;
    }

    public Integer getActualSlots(){
        return actualSlots;
    }

    public void setActualSlots(Integer actualSlots){
        this.actualSlots = actualSlots;
    }

    public void incrementActualSlots(){
        this.actualSlots += 1;
    }

    public void decrementActualSlots(){
        this.actualSlots -= 1;
    }

    public TournamentState getState(){
        return state;
    }

    public void setState(TournamentState state){
        this.state = state;
    }

    public Set<Court> getCourts(){
        return courts;
    }

    public void setCourts(Set<Court> courts){
        this.courts = courts;
    }
}
