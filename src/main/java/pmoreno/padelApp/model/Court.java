package pmoreno.padelApp.model;

public class Court {
    Long id;
    String prueba;
    
    public Court(Long id){
        this.id = id;
        this.prueba = "Magacela";
    }

    public Long getId(){
        return id;
    }

    public String getPrueba(){
        return prueba;
    }
}