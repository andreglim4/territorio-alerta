package com.territorio.alerta;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.PrevisaoComparacao;
import com.territorio.alerta.dominio.PrevisaoMediaMovel;
import com.territorio.alerta.dominio.ResultadoPrevisao;
import com.territorio.alerta.dominio.Tendencia;
import com.territorio.alerta.dominio.TipoSensor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoComparacaoTest {

    private final PrevisaoComparacao estrategia = new PrevisaoComparacao(3);

    @Test
    @DisplayName("Anterior 20 e recente 30: tendência +10 e previsão 40 (exemplo do enunciado)")
    void deveProjetarTendenciaDeAumento() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 18, 20, 22, 28, 30, 32);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertTrue(resultado.isConclusivo());
        assertEquals(40.0, resultado.getValorPrevisto(), 0.0001);
        assertEquals(6, resultado.getQuantidadeMedicoes());
        assertEquals(Tendencia.AUMENTO, resultado.getTendencia());
    }

    @Test
    @DisplayName("Anterior 60 e recente 50: tendência -10 e previsão 40")
    void deveProjetarTendenciaDeReducao() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 60, 60, 60, 50, 50, 50);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertEquals(40.0, resultado.getValorPrevisto(), 0.0001);
        assertEquals(Tendencia.REDUCAO, resultado.getTendencia());
    }

    @Test
    @DisplayName("Projeção é limitada à faixa física: chuva nunca negativa, umidade nunca acima de 100%")
    void deveLimitarProjecaoAFaixaDoSensor() {
        List<Medicao> chuvaParando = MedicoesDeTeste.sequencia("PLU-01", TipoSensor.PLUVIOMETRO, 30, 30, 30, 10, 10, 10);
        List<Medicao> soloSaturando = MedicoesDeTeste.sequencia("UMI-01", TipoSensor.UMIDADE_SOLO, 80, 80, 80, 95, 95, 95);

        ResultadoPrevisao chuva = estrategia.prever(chuvaParando);
        ResultadoPrevisao solo = estrategia.prever(soloSaturando);

        assertEquals(0.0, chuva.getValorPrevisto(), 0.0001);
        assertEquals(100.0, solo.getValorPrevisto(), 0.0001);
        assertTrue(solo.getExplicacao().contains("ajustada"));
    }

    @Test
    @DisplayName("Precisa de 2 períodos completos: com 5 medições não há previsão")
    void deveSilenciarComHistoricoInsuficiente() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 10, 20, 30, 40, 50);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertFalse(resultado.isConclusivo());
        assertEquals(6, estrategia.quantidadeNecessaria());
    }

    @Test
    @DisplayName("Polimorfismo: com o mesmo histórico, média móvel e comparação dão resultados diferentes")
    void estrategiasDevemTerComportamentosDiferentes() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 20, 20, 20, 30, 30, 30);

        double mediaMovel = new PrevisaoMediaMovel(5).prever(historico).getValorPrevisto();
        double comparacao = estrategia.prever(historico).getValorPrevisto();

        assertEquals(26.0, mediaMovel, 0.0001);
        assertEquals(40.0, comparacao, 0.0001);
        assertNotEquals(mediaMovel, comparacao);
    }
}
