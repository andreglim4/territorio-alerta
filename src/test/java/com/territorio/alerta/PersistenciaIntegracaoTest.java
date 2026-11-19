package com.territorio.alerta;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.Sensor;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.persistencia.AlertaRepository;
import com.territorio.alerta.persistencia.MedicaoRepository;
import com.territorio.alerta.servico.AnalisePrevisao;
import com.territorio.alerta.servico.ConfiguracaoSistema;
import com.territorio.alerta.servico.ServicoMonitoramento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PersistenciaIntegracaoTest {

    @Autowired
    private ServicoMonitoramento servico;
    @Autowired
    private MedicaoRepository medicaoRepository;
    @Autowired
    private AlertaRepository alertaRepository;

    @BeforeEach
    void limparBanco() {
        alertaRepository.deleteAll();
        medicaoRepository.deleteAll();
        ConfiguracaoSistema.getInstancia().restaurarPadroes();
    }

    @Test
    @DisplayName("Medições são gravadas e consultadas por sensor em ordem cronológica")
    void deveGravarEConsultarMedicoesPorSensor() {

        Sensor nivel = servico.buscarSensor("NIV-01");
        Sensor chuva = servico.buscarSensor("PLU-01");

        servico.registrarMedicao(nivel, 10.0);
        servico.registrarMedicao(chuva, 5.0);
        servico.registrarMedicao(nivel, 20.0);
        List<Medicao> historicoNivel = servico.consultarHistorico("NIV-01");

        assertEquals(3, medicaoRepository.count());
        assertEquals(2, historicoNivel.size());
        assertEquals(10.0, historicoNivel.get(0).getValor(), 0.0001);
        assertEquals(20.0, historicoNivel.get(1).getValor(), 0.0001);
        assertTrue(historicoNivel.get(0).getId() != null);
    }

    @Test
    @DisplayName("Valor inválido não chega ao banco")
    void naoDevePersistirMedicaoInvalida() {
        Sensor umidade = servico.buscarSensor("UMI-01");

        assertThrows(DadosInvalidosException.class, () -> servico.registrarMedicao(umidade, 150.0));

        assertEquals(0, medicaoRepository.count());
    }

    @Test
    @DisplayName("Fluxo completo: histórico do banco -> previsão -> alerta gravado e consultável")
    void deveGravarAlertaGeradoPelaPrevisao() {
        Sensor nivel = servico.buscarSensor("NIV-01");
        for (double valor : new double[]{10, 80, 82, 84, 86, 88}) {
            servico.registrarMedicao(nivel, valor);
        }

        // Act
        AnalisePrevisao analise = servico.executarPrevisao("NIV-01", TipoEstrategia.MEDIA_MOVEL);
        List<Alerta> alertas = servico.consultarAlertas();

        // Assert
        assertEquals(84.0, analise.resultado().getValorPrevisto(), 0.0001);
        assertEquals(1, alertas.size());
        assertEquals(NivelAlerta.CRITICO, alertas.get(0).getNivel());
        assertEquals(1, alertaRepository.countByNivel(NivelAlerta.CRITICO));
    }

    @Test
    @DisplayName("Dados gravados em H2 modo ARQUIVO continuam disponíveis após reiniciar a aplicação")
    void dadosDevemSobreviverAoReinicio() throws IOException {
        Path pasta = Paths.get("target", "teste-reinicio");
        apagar(pasta);
        String[] argumentos = {
                "--spring.datasource.url=jdbc:h2:file:./target/teste-reinicio/banco",
                "--spring.jpa.hibernate.ddl-auto=update",
                "--territorio.menu.habilitado=false"};

        try (ConfigurableApplicationContext primeira = iniciar(argumentos)) {
            ServicoMonitoramento s = primeira.getBean(ServicoMonitoramento.class);
            s.registrarMedicao(s.buscarSensor("PLU-01"), 33.3);
        }

        long total;
        double valor;
        try (ConfigurableApplicationContext segunda = iniciar(argumentos)) {
            MedicaoRepository repositorio = segunda.getBean(MedicaoRepository.class);
            total = repositorio.count();
            valor = repositorio.findBySensorIdOrderByDataHoraAscIdAsc("PLU-01").get(0).getValor();
        }

        assertEquals(1, total);
        assertEquals(33.3, valor, 0.0001);
    }

    private static ConfigurableApplicationContext iniciar(String[] argumentos) {
        SpringApplication aplicacao = new SpringApplicationBuilder(SistemaMonitoramento.class).build();
        return aplicacao.run(argumentos);
    }

    private static void apagar(Path pasta) throws IOException {
        if (!Files.exists(pasta)) {
            return;
        }
        try (Stream<Path> caminhos = Files.walk(pasta)) {
            for (Path p : caminhos.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
