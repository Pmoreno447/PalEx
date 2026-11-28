package pmoreno.padelApp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.checkout.Session;

import pmoreno.padelApp.dto.Inscription.InscriptionResponse;
import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
import pmoreno.padelApp.exceptions.ConflictException;
import pmoreno.padelApp.exceptions.ResourceNotFoundException;
import pmoreno.padelApp.model.BookingState;
import pmoreno.padelApp.model.Tournament;
import pmoreno.padelApp.model.TournamentRegistration;
import pmoreno.padelApp.model.User;
import pmoreno.padelApp.repository.TournamentInscriptionRepository;
import pmoreno.padelApp.repository.TournamentRepository;
import pmoreno.padelApp.repository.UserRepository;

@Service
public class TournamentInscriptionService {
    private static final Logger log = LoggerFactory.getLogger(TournamentInscriptionService.class);
    private final StripeService stripeService;
    private final TournamentInscriptionRepository tournamentInscriptionRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final int cancelDaysBefore;

    public TournamentInscriptionService(TournamentInscriptionRepository tournamentInscriptionRepository, TournamentRepository tournamentRepository,
                                        UserRepository userRepository, StripeService stripeService,
                                        @Value("${app.tournament.cancel-days-before}") int cancelDaysBefore){
        this.tournamentInscriptionRepository = tournamentInscriptionRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
        this.cancelDaysBefore = cancelDaysBefore;
    }

    @Transactional 
    public TournamentInscriptionResponse tournamentRegistration(String providerId, UUID tournamentId){
        // El usuario sale del token, nunca del body, para que nadie pueda inscribir a otro
        User user = userRepository.findByProviderId(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario " + providerId + " no sincronizado con la base de datos."));

        Tournament tournament = tournamentRepository.findByPublicId(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("El torneo con id " + tournamentId + "no existe"));


        if(tournament.getMaxSlots() != null && tournament.getActualSlots() >= tournament.getMaxSlots()){
            throw new ConflictException("El torneo " + tournament.getName() + " está lleno");
        }
        else{
            tournament.incrementActualSlots();
            TournamentRegistration tournamentRegistration = new TournamentRegistration(tournament, user, LocalDateTime.now(), tournament.getEntryPrice(), BookingState.PENDING);
            tournamentInscriptionRepository.save(tournamentRegistration);
    
            Session session = stripeService.tournamentPayment(tournamentRegistration);
    
            return new TournamentInscriptionResponse(tournamentRegistration.getPublicId(), session.getUrl());
        }

    }

    // Solo la encuentra si es del usuario: si no es suya se trata como inexistente (404)
    @Transactional(readOnly = true)
    public InscriptionResponse getInscription(String providerId, UUID inscriptionId){
        return InscriptionResponse.from(tournamentInscriptionRepository.findByPublicIdAndUserProviderId(inscriptionId, providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Inscripción con id " + inscriptionId + " no encontrada.")));
    }

    // readOnly: solo lee, y mantiene la sesión abierta para cargar el torneo (LAZY) de cada inscripción
    @Transactional(readOnly = true)
    public List<InscriptionResponse> getMyInscriptions(String providerId){
        // Filtrar por el providerId del token: cada usuario solo ve las suyas
        List<TournamentRegistration> inscriptions = tournamentInscriptionRepository.findByUserProviderIdOrderByRegisteredAtDesc(providerId);

        return inscriptions.stream()
            .map(InscriptionResponse::from)
            .toList();
    }
    
    @Transactional 
    public void tournamentInscriptionCancel(String providerId, UUID inscriptionId){
        TournamentRegistration tournamentRegistration = tournamentInscriptionRepository.findByPublicIdAndUserProviderId(inscriptionId, providerId)
            .orElseThrow(() -> new ResourceNotFoundException("No estás inscrito en este torneo"));

        // Plazo de cancelación: hasta cancelDaysBefore días antes del comienzo (torneo el 4 a las 9:00 y 2 días -> hasta el 2 a las 9:00).
        // Se comprueba aquí y no en Stripe, que reembolsa siempre que se le pide
        LocalDateTime deadline = tournamentRegistration.getTournament().getStartDate().minusDays(cancelDaysBefore);
        if(!LocalDateTime.now().isBefore(deadline)){
            throw new ConflictException("El plazo para cancelar la inscripción terminó el " + deadline);
        }

        if(tournamentRegistration.getState() == BookingState.COMPLETED){
            stripeService.tournamentRefund(tournamentRegistration);
            tournamentRegistration.setState(BookingState.CANCELLED);
            tournamentRegistration.getTournament().decrementActualSlots();
        }
        else{
            throw new ConflictException("El estado de la inscripción no permite su cancelación");
        }
    }

    @Transactional
    public void confirmInscription(UUID publicId, String paymentIntentId){
        resolvePending(publicId, BookingState.COMPLETED, paymentIntentId);
    }

    @Transactional
    public void expireInscription(UUID publicId){
        resolvePending(publicId, BookingState.EXPIRED, null);
    }

    // Stripe puede reenviar el mismo evento: solo se cambia el estado si la inscripción sigue pendiente
    private void resolvePending(UUID publicId, BookingState newState, String paymentIntentId){
        Optional<TournamentRegistration> tournamentInscription = tournamentInscriptionRepository.findByPublicId(publicId);

        if(tournamentInscription.isEmpty()){
            log.warn("Evento de pago para la inscripción {}, que no existe", publicId);
            return;
        }
        if(tournamentInscription.get().getState() == BookingState.PENDING){
            tournamentInscription.get().getTournament().decrementActualSlots();
            tournamentInscription.get().setState(newState);
            tournamentInscription.get().setPaymentIntentId(paymentIntentId);
        }

    }
}
