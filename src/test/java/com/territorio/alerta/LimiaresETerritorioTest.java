package com.territorio.alerta;

import com.territorio.alerta.dominio.DadosTerritorio;
import com.territorio.alerta.dominio.Limiares;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiaresETerritorioTest {

    @Test
    @DisplayName("Classificação: prevalece o nível mais alto atingido e valores iguais ao limiar contam")
    void deveClassificarPeloNivelMaisAlto() {
        Limiares limiares = new Limiares(10.0, 25.0, 50.0);

        Optional<NivelAlerta> abaixo = limiares.classificar(9.9);
        Optional<NivelAlerta> igualAtencao = limiares.classificar(10.0);
        Optional<NivelAlerta> entreAlertaECritico = limiares.classificar(49.9);
        Optional<NivelAlerta> igualCritico = limiares.classificar(50.0);

        assertTrue(abaixo.isEmpty());
        assertEquals(NivelAlerta.ATENCAO, igualAtencao.orElseThrow());
        assertEquals(NivelAlerta.ALERTA, entreAlertaECritico.orElseThrow());
        assertEquals(NivelAlerta.CRITICO, igualCritico.orElseThrow());
    }

    @Test
    @DisplayName("Limiares fora de ordem ou negativos são rejeitados")
    void deveRejeitarLimiaresInvalidos() {
        assertThrows(DadosInvalidosException.class, () -> new Limiares(50.0, 25.0, 80.0)); // atenção > alerta
        assertThrows(DadosInvalidosException.class, () -> new Limiares(10.0, 10.0, 80.0)); // iguais
        assertThrows(DadosInvalidosException.class, () -> new Limiares(-5.0, 25.0, 80.0)); // negativo
    }

    @Test
    @DisplayName("DadosTerritorio protege seus dados: limiar acima da faixa e população negativa são recusados")
    void territorioDeveValidarAlteracoes() {
        DadosTerritorio territorio = new DadosTerritorio("REG-T", "Região Teste", 1000, 2.0, 3);
        territorio.definirLimiares(TipoSensor.UMIDADE_SOLO, 70, 85, 95);

        DadosInvalidosException acimaDaFaixa = assertThrows(DadosInvalidosException.class,
                () -> territorio.definirLimiares(TipoSensor.UMIDADE_SOLO, 80, 100, 120));
        assertThrows(DadosInvalidosException.class, () -> territorio.setPopulacaoEstimada(-1));

        assertTrue(acimaDaFaixa.getMessage().contains("acima do valor máximo"));
        assertEquals(95.0, territorio.getLimiares(TipoSensor.UMIDADE_SOLO).getCritico(), 0.0001);
        assertEquals(1000, territorio.getPopulacaoEstimada());
    }
}
