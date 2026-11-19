package com.territorio.alerta;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.PontoMonitoramento;
import com.territorio.alerta.dominio.Sensor;
import com.territorio.alerta.dominio.SensorNivelAgua;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.excecao.FonteIndisponivelException;
import com.territorio.alerta.persistencia.AlertaRepository;
import com.territorio.alerta.persistencia.MedicaoRepository;
import com.territorio.alerta.servico.AnalisePrevisao;
import com.territorio.alerta.servico.AvaliadorAlertas;
import com.territorio.alerta.servico.ConfiguracaoSistema;
import com.territorio.alerta.servico.FabricaPrevisao;
import com.territorio.alerta.servico.ServicoMonitoramento;
import com.territorio.alerta.util.ExportadorCSV;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicoMonitoramentoTest {

    @Mock
    private MedicaoRepository medicaoRepository;
    @Mock
    private AlertaRepository alertaRepository;
    @Mock
    private ExportadorCSV exportadorCSV;

    private final ConfiguracaoSistema configuracao = ConfiguracaoSistema.getInstancia();
    private PontoMonitoramento ponto;
    private ServicoMonitoramento servico;

    @BeforeEach
    void preparar() {
        configuracao.restaurarPadroes();
        ponto = new PontoMonitoramento("PM-01", "Ponto Teste", "Região Teste");
        ponto.adicionarSensor(new SensorNivelAgua("NIV-01", new Random(1), 0, 0));
        servico = new ServicoMonitoramento(medicaoRepository, alertaRepository, new FabricaPrevisao(configuracao),
                configuracao, ponto, new AvaliadorAlertas(), exportadorCSV);
    }

    @AfterEach
    void limpar() {
        configuracao.restaurarPadroes();
    }

    private void historicoDoBanco(double... valores) {
        List<Medicao> historico = new ArrayList<>(MedicoesDeTeste.sequencia("NIV-01", TipoSensor.NIVEL_AGUA, valores));
        Collections.reverse(historico);
        when(medicaoRepository.findBySensorIdOrderByDataHoraDescIdDesc(eq("NIV-01"), any(Limit.class))).thenReturn(historico);
    }

    @Test
    @DisplayName("Previsão acima do limiar crítico gera e GRAVA um alerta CRÍTICO explicado")
    void deveGerarAlertaCriticoQuandoLimiarUltrapassado() {
        historicoDoBanco(80, 82, 84, 86, 88);
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.MEDIA_MOVEL);

        ArgumentCaptor<Alerta> alertaGravado = ArgumentCaptor.forClass(Alerta.class);
        verify(alertaRepository).save(alertaGravado.capture());
        Alerta alerta = alertaGravado.getValue();
        assertEquals(NivelAlerta.CRITICO, alerta.getNivel());
        assertEquals(84.0, alerta.getValorPrevisto(), 0.0001);
        assertEquals(80.0, alerta.getLimiteUltrapassado(), 0.0001);
        assertTrue(alerta.getExplicacao().contains("CRÍTICO"));
        assertTrue(alerta.getExplicacao().contains("NIV-01"));
        assertTrue(alerta.getExplicacao().contains("84,0 cm"));
        assertTrue(alerta.getExplicacao().contains("80,0 cm"));
        assertTrue(analise.alertaEmitido().isPresent());
    }

    @Test
    @DisplayName("Previsão entre os limiares de alerta e crítico gera nível ALERTA (o mais alto atingido)")
    void deveClassificarNoNivelMaisAltoAtingido() {
        historicoDoBanco(50, 51, 52, 53, 54);
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.MEDIA_MOVEL);

        assertEquals(NivelAlerta.ALERTA, analise.alerta().getNivel());
        assertEquals(50.0, analise.alerta().getLimiteUltrapassado(), 0.0001);
    }

    @Test
    @DisplayName("Previsão abaixo de todos os limiares NÃO gera nem grava alerta")
    void naoDeveGerarAlertaAbaixoDosLimiares() {
        historicoDoBanco(10, 12, 11, 13, 14);

        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.MEDIA_MOVEL);

        assertTrue(analise.resultado().isConclusivo());
        assertEquals(12.0, analise.resultado().getValorPrevisto(), 0.0001);
        assertTrue(analise.alertaEmitido().isEmpty());
        verify(alertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem histórico suficiente não há previsão e nenhum alerta é gravado, mesmo com valores altos")
    void naoDeveAlertarSemHistoricoSuficiente() {
        historicoDoBanco(95, 99);

        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.MEDIA_MOVEL);

        assertFalse(analise.resultado().isConclusivo());
        verify(alertaRepository, never()).save(any());
    }

    @Test
    @DisplayName("A estratégia vem da Factory: comparação pede 6 medições ao repositório e projeta a tendência")
    void deveUsarEstrategiaCriadaPelaFabrica() {
        historicoDoBanco(20, 20, 20, 30, 30, 30);
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.COMPARACAO);

        ArgumentCaptor<Limit> limite = ArgumentCaptor.forClass(Limit.class);
        verify(medicaoRepository).findBySensorIdOrderByDataHoraDescIdDesc(eq("NIV-01"), limite.capture());
        assertEquals(6, limite.getValue().max());
        assertEquals(40.0, analise.resultado().getValorPrevisto(), 0.0001);
        assertEquals(NivelAlerta.ATENCAO, analise.alerta().getNivel());
    }

    @Test
    @DisplayName("Leitura válida do sensor é gravada no repositório")
    void deveGravarMedicaoValida() {
        Sensor sensor = sensorSimulado(true);
        when(sensor.lerValor()).thenReturn(12.5);
        when(medicaoRepository.save(any(Medicao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Medicao medicao = servico.realizarLeitura("NIV-02");

        ArgumentCaptor<Medicao> gravada = ArgumentCaptor.forClass(Medicao.class);
        verify(medicaoRepository).save(gravada.capture());
        assertEquals(12.5, gravada.getValue().getValor(), 0.0001);
        assertEquals("NIV-02", gravada.getValue().getSensorId());
        assertEquals("PM-01", medicao.getPontoId());
    }

    @Test
    @DisplayName("Leitura inválida lança DadosInvalidosException e NADA é gravado")
    void naoDeveGravarMedicaoInvalida() {
        Sensor sensor = sensorSimulado(true);
        when(sensor.lerValor()).thenReturn(-8.0);

        assertThrows(DadosInvalidosException.class, () -> servico.realizarLeitura("NIV-02"));
        verify(medicaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sensor inoperante lança FonteIndisponivelException, não é lido e nada é gravado")
    void deveFalharComSensorInoperante() {
        Sensor sensor = sensorSimulado(false);

        FonteIndisponivelException erro = assertThrows(FonteIndisponivelException.class,
                () -> servico.realizarLeitura("NIV-02"));
        assertTrue(erro.getMessage().contains("NIV-02"));
        verify(sensor, never()).lerValor();
        verify(medicaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sensor inexistente é informado com DadosInvalidosException")
    void deveRecusarSensorInexistente() {
        String sensorInexistente = "XYZ-99";

        assertThrows(DadosInvalidosException.class, () -> servico.executarPrevisao(sensorInexistente, TipoEstrategia.MEDIA_MOVEL));
        verify(alertaRepository, never()).save(any());
    }

    private Sensor sensorSimulado(boolean funcionando) {
        Sensor sensor = mock(Sensor.class);
        when(sensor.getId()).thenReturn("NIV-02");
        when(sensor.estaFuncionando()).thenReturn(funcionando);
        if (funcionando) {
            when(sensor.getTipo()).thenReturn(TipoSensor.NIVEL_AGUA);
            when(sensor.getUnidade()).thenReturn("cm");
        }
        ponto.adicionarSensor(sensor);
        return sensor;
    }
}
