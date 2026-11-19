package com.territorio.alerta;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.PluviometroSensor;
import com.territorio.alerta.dominio.PontoMonitoramento;
import com.territorio.alerta.dominio.Sensor;
import com.territorio.alerta.dominio.SensorNivelAgua;
import com.territorio.alerta.dominio.SensorUmidadeSolo;
import com.territorio.alerta.dominio.StatusComponente;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.excecao.FonteIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorTest {

    @Test
    @DisplayName("Os três sensores produzem leituras dentro da faixa física do seu tipo")
    void sensoresDevemLerDentroDaFaixa() {
        List<Sensor> sensores = List.of(
                new PluviometroSensor("PLU-T", new Random(1), 0, 0),
                new SensorNivelAgua("NIV-T", new Random(2), 0, 0),
                new SensorUmidadeSolo("UMI-T", new Random(3), 0, 0));

        for (Sensor sensor : sensores) {
            for (int i = 0; i < 300; i++) {
                // Act
                double valor = sensor.lerValor();
                // Assert
                assertTrue(sensor.getTipo().aceita(valor), sensor.getId() + " leu valor fora da faixa: " + valor);
            }
        }
    }

    @Test
    @DisplayName("Sensor inoperante ou com falha de comunicação lança FonteIndisponivelException")
    void sensorIndisponivelDeveLancarExcecao() {
        Sensor emManutencao = new SensorNivelAgua("NIV-T", new Random(1), 0, 0);
        emManutencao.definirOperante(false);
        Sensor comFalha = new PluviometroSensor("PLU-T", new Random(1), 1.0, 0);

        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class, emManutencao::lerValor);
        assertTrue(erro.getMessage().contains("inoperante"));
        assertThrows(FonteIndisponivelException.class, comFalha::lerValor);
    }

    @Test
    @DisplayName("Leitura espúria do sensor é barrada pela validação da Medicao")
    void leituraEspuriaDeveSerRejeitada() {
        Sensor comDefeito = new SensorUmidadeSolo("UMI-T", new Random(1), 0, 1.0);
        double lido = comDefeito.lerValor();

        assertThrows(DadosInvalidosException.class, () -> new Medicao(comDefeito.getId(), TipoSensor.UMIDADE_SOLO,
                "PM-01", "Região", lido, "%", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Ponto de monitoramento calcula o próprio status a partir dos sensores")
    void pontoDeveVerificarStatusPelosSensores() {
        PontoMonitoramento ponto = new PontoMonitoramento("PM-T", "Ponto Teste", "Região Teste");
        Sensor chuva = new PluviometroSensor("PLU-T", new Random(1), 0, 0);
        Sensor nivel = new SensorNivelAgua("NIV-T", new Random(1), 0, 0);

        assertEquals(StatusComponente.SEM_SENSORES, ponto.verificarStatus());
        ponto.adicionarSensor(chuva);
        ponto.adicionarSensor(nivel);
        assertEquals(StatusComponente.OPERACIONAL, ponto.verificarStatus());
        nivel.definirOperante(false);
        assertEquals(StatusComponente.DEGRADADO, ponto.verificarStatus());
        chuva.definirOperante(false);
        assertEquals(StatusComponente.INOPERANTE, ponto.verificarStatus());
        assertThrows(DadosInvalidosException.class, () -> ponto.adicionarSensor(new SensorNivelAgua("NIV-T")));
    }
}
