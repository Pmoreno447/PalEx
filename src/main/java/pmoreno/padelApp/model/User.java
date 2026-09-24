package pmoreno.padelApp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table (name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = true, length = 100)
    private String name;

    // Debe venir en el token sirve para asociar cada usuario
    // de la base de datos de usuarios con nuestra base de datos 
    // donde guardamos la información de cada uno.
    @Column(unique = true, nullable = false, length = 255)
    private String providerId;

    // Debe venir en el token
    @Column(length = 255, unique = true)
    private String email;

    @Column (length = 20, unique = true)
    private String username;

    // Número de teléfono
    @Column (length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected User() { }          // exigido por JPA

    public User(String providerId, String email, Role role) {
        this.providerId = providerId;
        this.email = email;
        this.role = role;
    }

    public User(Long id, String name, String tphNumber){
        this.id = id;
        this.name = name;
        this.phone = tphNumber;
    }

    public Long getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String tphNumber) {
        this.phone = tphNumber;
    }
}

