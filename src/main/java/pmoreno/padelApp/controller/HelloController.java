package pmoreno.padelApp.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
public class HelloController {
    
    @GetMapping("/hola")
    public String getHandshake(@RequestParam(value = "nombre", defaultValue = "chico") String param) {
        return new String("Hola " + param + " empecemos el proyecto");
    }
    
}
