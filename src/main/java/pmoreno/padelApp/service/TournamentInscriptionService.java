package pmoreno.padelApp.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.checkout.Session;

import pmoreno.padelApp.dto.Inscription.TournamentInscriptionResponse;
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

    public TournamentInscriptionService(TournamentInscriptionRepository tournamentInscriptionRepository, TournamentRepository tournamentRepository,
                                        UserRepository userRepository, StripeService stripeService){
        this.tournamentInscriptionRepository = tournamentInscriptionRepository;
        this.tournamentRepository = tournamentRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    @Transactional 
    public TournamentInscriptionResponse tournamentRegistration(String providerId, UUID tournamentId){
        // El usuario sale del token, nunca del body, para que nadie pueda inscribir a otro
        User user = userRepository.findByProviderId(providerId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario " + providerId + " no sincronizado con la base de datos."));

        Tournament tournament = tournamentRepository.findByPublicId(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("El torneo con id " + tournamentId + "no existe"));


        TournamentRegistration tournamentRegistration = new TournamentRegistration(tournament, user, LocalDateTime.now(), tournament.getEntryPrice(), BookingState.PENDING);
        tournamentInscriptionRepository.save(tournamentRegistration);

        Session session = stripeService.tournamentPayment(tournamentRegistration);

        return new TournamentInscriptionResponse(tournamentRegistration.getPublicId(), session.getUrl());
    }

    @Transactional
    public void confirmInscription(UUID publicId){
        resolvePending(publicId, BookingState.COMPLETED);
    }

    @Transactional
    public void expireInscription(UUID publicId){
        resolvePending(publicId, BookingState.EXPIRED);
    }

    // Stripe puede reenviar el mismo evento: solo se cambia el estado si la inscripción sigue pendiente
    private void resolvePending(UUID publicId, BookingState newState){
        Optional<TournamentRegistration> tournamentInscription = tournamentInscriptionRepository.findByPublicId(publicId);

        if(tournamentInscription.isEmpty()){
            log.warn("Evento de pago para la inscripción {}, que no existe", publicId);
            return;
        }
        if(tournamentInscription.get().getState() == BookingState.PENDING){
            tournamentInscription.get().setState(newState);
        }

    }
}
