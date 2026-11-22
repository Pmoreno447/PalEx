package pmoreno.padelApp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.model.checkout.Session;

import pmoreno.padelApp.dto.Inscription.InscriptionResponse;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Role;
import pmoreno.padelApp.model.Tournament;
import pmoreno.padelApp.model.TournamentRegistration;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.TournamentInscriptionRepository;
import pmoreno.padelApp.repository.TournamentRepository;
import pmoreno.padelApp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class TournamentInscriptionServiceTests {
    @Mock
    private TournamentInscriptionRepository tournamentInscriptionRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeService stripeService;

    private TournamentInscriptionService tournamentInscriptionService;

    private User userTest;

    private Tournament tournamentTest;

    @BeforeEach
    void setUp(){
        tournamentInscriptionService = new TournamentInscriptionService(tournamentInscriptionRepository, tournamentRepository,
                                                                        userRepository, stripeService, 2);
        userTest = new User(1L, "Test Name", "user-1", "test@example.com", "CoolTestUser", "+34123456789", Role.USER);
        LocalDateTime start = LocalDateTime.now().plusDays(7);
        tournamentTest = new Tournament("Torneo Test", "**Torneo** de prueba", start, start.plusHours(10),
                                        new BigDecimal("15.00"), 16, new HashSet<>());
    }

    @Test
    @DisplayName("Should save a pending inscription with the tournament price and return the checkout url")
    void shouldCreateInscription_whenTournamentExists(){
        when(userRepository.findByProviderId("user-1")).thenReturn(Optional.of(userTest));
        when(tournamentRepository.findByPublicId(tournamentTest.getPublicId())).thenReturn(Optional.of(tournamentTest));
        Session session = new Session();
        session.setUrl("https://checkout.test/session");
        when(stripeService.tournamentPayment(any(TournamentRegistration.class))).thenReturn(session);

        TournamentInscriptionResponse response =
            tournamentInscriptionService.tournamentRegistration("user-1", tournamentTest.getPublicId());

        ArgumentCaptor<TournamentRegistration> captor = ArgumentCaptor.forClass(TournamentRegistration.class);
        verify(tournamentInscriptionRepository).save(captor.capture());
        TournamentRegistration saved = captor.getValue();

        assertEquals(BookingState.PENDING, saved.getState());
        assertEquals(new BigDecimal("15.00"), saved.getAmount());
        assertEquals(userTest, saved.getUser());
        assertEquals(tournamentTest, saved.getTournament());
        assertEquals(saved.getPublicId(), response.id());
        assertEquals("https://checkout.test/session", response.checkoutUrl());
    }

    @Test
    @DisplayName("Should not create an inscription when the tournament does not exist")
    void shouldNotCreateInscription_whenTournamentDoesNotExist(){
        UUID tournamentId = UUID.randomUUID();
        when(userRepository.findByProviderId("user-1")).thenReturn(Optional.of(userTest));
        when(tournamentRepository.findByPublicId(tournamentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            tournamentInscriptionService.tournamentRegistration("user-1", tournamentId)
        );

        verify(tournamentInscriptionRepository, never()).save(any());
        verify(stripeService, never()).tournamentPayment(any());
    }

    // ---------- Cancelar ----------

    // Inscripción de userTest en un torneo que empieza dentro de startInDays días
    private TournamentRegistration inscription(BookingState state, int startInDays){
        LocalDateTime start = LocalDateTime.now().plusDays(startInDays);
        Tournament tournament = new Tournament("Torneo Test", "**Torneo** de prueba", start, start.plusHours(10),
                                               new BigDecimal("15.00"), 16, new HashSet<>());
        return new TournamentRegistration(tournament, userTest, LocalDateTime.now(), new BigDecimal("15.00"), state);
    }

    @Test
    @DisplayName("Should refund and cancel a paid inscription within the cancellation period")
    void shouldCancelInscription_whenPaidAndOnTime(){
        TournamentRegistration registration = inscription(BookingState.COMPLETED, 7);
        when(tournamentInscriptionRepository.findByPublicIdAndUserProviderId(registration.getPublicId(), "user-1"))
            .thenReturn(Optional.of(registration));

        tournamentInscriptionService.tournamentInscriptionCancel("user-1", registration.getPublicId());

        verify(stripeService).tournamentRefund(registration);
        assertEquals(BookingState.CANCELLED, registration.getState());
    }

    @Test
    @DisplayName("Should not cancel when the cancellation period is over")
    void shouldNotCancelInscription_whenPeriodIsOver(){
        // Empieza mañana y el plazo es de 2 días: ya no se puede cancelar
        TournamentRegistration registration = inscription(BookingState.COMPLETED, 1);
        when(tournamentInscriptionRepository.findByPublicIdAndUserProviderId(registration.getPublicId(), "user-1"))
            .thenReturn(Optional.of(registration));

        assertThrows(ConflictException.class, () ->
            tournamentInscriptionService.tournamentInscriptionCancel("user-1", registration.getPublicId())
        );

        verify(stripeService, never()).tournamentRefund(any());
        assertEquals(BookingState.COMPLETED, registration.getState());
    }

    @Test
    @DisplayName("Should not cancel an inscription that has not been paid")
    void shouldNotCancelInscription_whenNotPaid(){
        TournamentRegistration registration = inscription(BookingState.PENDING, 7);
        when(tournamentInscriptionRepository.findByPublicIdAndUserProviderId(registration.getPublicId(), "user-1"))
            .thenReturn(Optional.of(registration));

        assertThrows(ConflictException.class, () ->
            tournamentInscriptionService.tournamentInscriptionCancel("user-1", registration.getPublicId())
        );

        verify(stripeService, never()).tournamentRefund(any());
    }

    // ---------- Consultar ----------

    @Test
    @DisplayName("Should return the inscription when it belongs to the user")
    void shouldGetInscription_whenUserIsOwner(){
        TournamentRegistration registration = inscription(BookingState.COMPLETED, 7);
        when(tournamentInscriptionRepository.findByPublicIdAndUserProviderId(registration.getPublicId(), "user-1"))
            .thenReturn(Optional.of(registration));

        InscriptionResponse response = tournamentInscriptionService.getInscription("user-1", registration.getPublicId());

        assertEquals(registration.getPublicId(), response.id());
        assertEquals(registration.getTournament().getPublicId(), response.tournamentId());
        assertEquals("Torneo Test", response.tournamentName());
    }

    @Test
    @DisplayName("Should not find an inscription that does not exist or belongs to another user")
    void shouldNotGetInscription_whenNotFoundForUser(){
        UUID inscriptionId = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () ->
            tournamentInscriptionService.getInscription("user-1", inscriptionId)
        );
    }

    @Test
    @DisplayName("Should return every inscription of the user")
    void shouldGetMyInscriptions(){
        TournamentRegistration paid = inscription(BookingState.COMPLETED, 7);
        TournamentRegistration pending = inscription(BookingState.PENDING, 10);
        when(tournamentInscriptionRepository.findByUserProviderIdOrderByRegisteredAtDesc("user-1"))
            .thenReturn(List.of(paid, pending));

        List<InscriptionResponse> response = tournamentInscriptionService.getMyInscriptions("user-1");

        assertEquals(2, response.size());
        assertEquals(paid.getPublicId(), response.get(0).id());
        assertEquals(BookingState.PENDING, response.get(1).state());
    }
}
