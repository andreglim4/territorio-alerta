package com.territorio.alerta;

import com.territorio.alerta.console.MenuPrincipal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SistemaMonitoramento {

    public static void main(String[] args) {
        SpringApplication.run(SistemaMonitoramento.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "territorio.menu.habilitado", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner iniciarMenu(MenuPrincipal menuPrincipal) {
        return args -> menuPrincipal.executar();
    }
}
