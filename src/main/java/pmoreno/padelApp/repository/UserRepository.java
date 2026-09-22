package pmoreno.padelApp.repository;

import org.springframework.data.repository.CrudRepository;

import pmoreno.padelApp.model.User;
import java.util.Optional;


public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByProviderId(String providerId);
}


