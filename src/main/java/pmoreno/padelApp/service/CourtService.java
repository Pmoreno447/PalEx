package pmoreno.padelApp.service;

import java.util.List;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
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
    
}
