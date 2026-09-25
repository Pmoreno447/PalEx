package pmoreno.padelApp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import pmoreno.padelApp.dto.CourtRequest;
import pmoreno.padelApp.dto.CourtResponse;
import pmoreno.padelApp.model.Court;
import pmoreno.padelApp.repository.CourtRepository;

@Service 
public class CourtService {
    private final CourtRepository courtRepository;

    public CourtService(CourtRepository courtRepository){
        this.courtRepository = courtRepository;
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getCourts(boolean isAdmin){
        List<Court> courts = isAdmin
            ? courtRepository.findAll()
            : courtRepository.findByActiveTrue();

        return courts.stream()
            .map(CourtResponse::from)
            .toList();
    }

    @Transactional 
    public CourtResponse createCourt(CourtRequest courtRequest){
        Court c =  courtRepository.save(courtRequest.toModel());

        return CourtResponse.from(c);
    }

    @Transactional 
    public CourtResponse updateCourt(Long courtId, CourtRequest courtRequest){
        Court c = courtRepository.findById(courtId)
            .orElseThrow(() -> new IllegalStateException("[updateCourt]: Pista no encontrada"));

        if(courtRequest.name() != null){
            c.setName(courtRequest.name());
        }
        if(courtRequest.price() != null){
            c.setPrice(courtRequest.price());
        }
        if(courtRequest.active() != null){
            c.setActive(courtRequest.active());
        }
        if(courtRequest.slotMinutes() != null){
            c.setSlotMinutes(courtRequest.slotMinutes());
        }
        if(courtRequest.openTime() != null){
            c.setOpenTime(courtRequest.openTime());
        }
        if(courtRequest.closTime() != null){
            c.setCloseTime(courtRequest.closTime());
        }

        return CourtResponse.from(c);


    }
    
}
