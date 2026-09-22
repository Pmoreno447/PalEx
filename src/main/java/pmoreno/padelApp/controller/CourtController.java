package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pmoreno.padelApp.model.Court;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/courts")
public class CourtController {

    @GetMapping("/{courtId}")
    public Court getCourtDisponibility(@PathVariable String courtId) {
        Court c1 = new Court(Long.valueOf("1"));
        
        return c1;
    }

    @PostMapping
    public Court postMethodName(@RequestBody Court entity) {
        //TODO: process POST request
        
        return new Court(Long.valueOf(entity.getId()));
    }
    
}
