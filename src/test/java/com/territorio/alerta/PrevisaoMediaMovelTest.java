package com.territorio.alerta;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.PrevisaoMediaMovel;
import com.territorio.alerta.dominio.ResultadoPrevisao;
import com.territorio.alerta.dominio.Tendencia;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrevisaoMediaMovelTest {

    private final PrevisaoMediaMovel estrategia = new PrevisaoMediaMovel(5);

    @Test
    @DisplayName("Média de 20, 25, 30, 35 e 40 é 30 (exemplo do enunciado)")
    void deveCalcularMediaMovel() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("PLU-01", TipoSensor.PLUVIOMETRO, 20, 25, 30, 35, 40);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertTrue(resultado.isConclusivo());
        assertEquals(30.0, resultado.getValorPrevisto(), 0.0001);
        assertEquals(5, resultado.getQuantidadeMedicoes());
        assertEquals("mm/h", resultado.getUnidade());
        assertEquals(Tendencia.AUMENTO, resultado.getTendencia());
        assertEquals("Média móvel", resultado.getEstrategia());
    }

    @Test
    @DisplayName("Usa somente as N medições mais recentes, mesmo que o histórico venha em ordem decrescente")
    void deveUsarSomenteAsUltimasMedicoesEmOrdemCronologica() {
        List<Medicao> historico = new ArrayList<>(
                MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 100, 100, 20, 25, 30, 35, 40));
        Collections.reverse(historico);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertEquals(30.0, resultado.getValorPrevisto(), 0.0001);
        assertEquals(Tendencia.AUMENTO, resultado.getTendencia());
    }

    @Test
    @DisplayName("Com menos de 5 medições o sistema se cala: não há valor previsto")
    void deveSilenciarComHistoricoInsuficiente() {
        List<Medicao> historico = MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 90, 95, 99);

        ResultadoPrevisao resultado = estrategia.prever(historico);

        assertFalse(resultado.isConclusivo());
        assertNull(resultado.getValorPrevisto());
        assertEquals(ResultadoPrevisao.Situacao.HISTORICO_INSUFICIENTE, resultado.getSituacao());
        assertTrue(resultado.getExplicacao().contains("Histórico insuficiente"));
    }

    @Test
    @DisplayName("Histórico que mistura sensores diferentes lança DadosInvalidosException")
    void deveRejeitarMedicoesDeSensoresDiferentes() {
        List<Medicao> historico = new ArrayList<>(MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, 10, 11, 12));
        historico.addAll(MedicoesDeTeste.sequencia("PLU-01", TipoSensor.PLUVIOMETRO, 5, 6));

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () -> estrategia.prever(historico));
        assertTrue(erro.getMessage().contains("mistura"));
    }

    @Test
    @DisplayName("Janela menor que 5 medições é recusada")
    void deveRecusarJanelaPequena() {
        int janelaPequena = 2;

        assertThrows(DadosInvalidosException.class, () -> new PrevisaoMediaMovel(janelaPequena));
    }
}
