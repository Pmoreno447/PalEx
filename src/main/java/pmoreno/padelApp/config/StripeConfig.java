package pmoreno.padelApp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stripe.StripeClient;

@Configuration 
public class StripeConfig {
    @Bean
    StripeClient stripeClient(@Value("${stripe.secret-key}") String secretKey){
        return new StripeClient(secretKey);
    }
}
