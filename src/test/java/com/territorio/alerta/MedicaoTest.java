package com.territorio.alerta;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.now().minusMinutes(1);

    @Test
    @DisplayName("Medição válida é criada com os valores e a unidade do sensor")
    void deveCriarMedicaoValida() {
        double valorLido = 42.5;

        Medicao medicao = new Medicao("NIV-01", TipoSensor.NIVEL_AGUA, "PM-01", "Região X", valorLido, "cm", AGORA);

        assertEquals("NIV-01", medicao.getSensorId());
        assertEquals(TipoSensor.NIVEL_AGUA, medicao.getTipoSensor());
        assertEquals(42.5, medicao.getValor(), 0.0001);
        assertEquals("cm", medicao.getUnidade());
        assertEquals(AGORA, medicao.getDataHora());
    }

    @Test
    @DisplayName("Medição de chuva negativa é rejeitada com DadosInvalidosException")
    void deveRejeitarChuvaNegativa() {
        double valorNegativo = -3.0;

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () ->
                new Medicao("PLU-01", TipoSensor.PLUVIOMETRO, "PM-01", "Região X", valorNegativo, "mm/h", AGORA));

        assertTrue(erro.getMessage().contains("negativa"), "a mensagem deve explicar o motivo: " + erro.getMessage());
        assertTrue(erro.getMessage().contains("PLU-01"));
    }

    @Test
    @DisplayName("Umidade do solo acima de 100% está fora da faixa e é rejeitada")
    void deveRejeitarValorForaDaFaixa() {
        double umidadeImpossivel = 120.0;

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () ->
                new Medicao("UMI-01", TipoSensor.UMIDADE_SOLO, "PM-01", "Região X", umidadeImpossivel, "%", AGORA));
        assertTrue(erro.getMessage().contains("fora da faixa"));
    }

    @Test
    @DisplayName("Unidade incompatível com o tipo do sensor é rejeitada")
    void deveRejeitarUnidadeIncompativel() {
        String unidadeErrada = "mm/h";

        DadosInvalidosException erro = assertThrows(DadosInvalidosException.class, () ->
                new Medicao("NIV-01", TipoSensor.NIVEL_AGUA, "PM-01", "Região X", 10.0, unidadeErrada, AGORA));
        assertTrue(erro.getMessage().contains("Unidade incompatível"));
    }

    @Test
    @DisplayName("Valor não numérico (NaN) e data no futuro são rejeitados")
    void deveRejeitarNaNEDataFutura() {
        LocalDateTime amanha = LocalDateTime.now().plusDays(1);

        assertThrows(DadosInvalidosException.class, () ->
                new Medicao("NIV-01", TipoSensor.NIVEL_AGUA, "PM-01", "Região X", Double.NaN, "cm", AGORA));
        assertThrows(DadosInvalidosException.class, () ->
                new Medicao("NIV-01", TipoSensor.NIVEL_AGUA, "PM-01", "Região X", 10.0, "cm", amanha));
    }
}
