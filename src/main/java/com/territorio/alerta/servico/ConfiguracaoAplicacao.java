package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.DadosTerritorio;
import com.territorio.alerta.dominio.PluviometroSensor;
import com.territorio.alerta.dominio.PontoMonitoramento;
import com.territorio.alerta.dominio.SensorNivelAgua;
import com.territorio.alerta.dominio.SensorUmidadeSolo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoAplicacao {

    @Bean
    public ConfiguracaoSistema configuracaoSistema() {
        return ConfiguracaoSistema.getInstancia();
    }

    @Bean
    public FabricaPrevisao fabricaPrevisao(ConfiguracaoSistema configuracaoSistema) {
        return new FabricaPrevisao(configuracaoSistema);
    }

    @Bean
    public PontoMonitoramento pontoMonitoramento(ConfiguracaoSistema configuracaoSistema) {
        DadosTerritorio territorio = configuracaoSistema.getDadosTerritorio();
        PontoMonitoramento ponto = new PontoMonitoramento("PM-01",
                "Ponto de Monitoramento A (margem do córrego)", territorio.getNomeRegiao());
        ponto.adicionarSensor(new PluviometroSensor("PLU-01"));
        ponto.adicionarSensor(new SensorNivelAgua("NIV-01"));
        ponto.adicionarSensor(new SensorUmidadeSolo("UMI-01"));
        ponto.verificarStatus();
        return ponto;
    }
}
