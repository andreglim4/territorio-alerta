package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.EstrategiaPrevisao;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.PontoMonitoramento;
import com.territorio.alerta.dominio.ResultadoPrevisao;
import com.territorio.alerta.dominio.Sensor;
import com.territorio.alerta.dominio.StatusComponente;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.excecao.FonteIndisponivelException;
import com.territorio.alerta.persistencia.AlertaRepository;
import com.territorio.alerta.persistencia.MedicaoRepository;
import com.territorio.alerta.util.ExportadorCSV;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ServicoMonitoramento {

    private static final int MAXIMO_PREVISOES_RECENTES = 6;

    private final MedicaoRepository medicaoRepository;
    private final AlertaRepository alertaRepository;
    private final FabricaPrevisao fabricaPrevisao;
    private final ConfiguracaoSistema configuracao;
    private final PontoMonitoramento ponto;
    private final AvaliadorAlertas avaliadorAlertas;
    private final ExportadorCSV exportadorCSV;

    private final Deque<AnalisePrevisao> previsoesRecentes = new ArrayDeque<>();

    public ServicoMonitoramento(MedicaoRepository medicaoRepository, AlertaRepository alertaRepository,
                                FabricaPrevisao fabricaPrevisao, ConfiguracaoSistema configuracao,
                                PontoMonitoramento ponto, AvaliadorAlertas avaliadorAlertas,
                                ExportadorCSV exportadorCSV) {
        this.medicaoRepository = medicaoRepository;
        this.alertaRepository = alertaRepository;
        this.fabricaPrevisao = fabricaPrevisao;
        this.configuracao = configuracao;
        this.ponto = ponto;
        this.avaliadorAlertas = avaliadorAlertas;
        this.exportadorCSV = exportadorCSV;
    }

    
    public List<Sensor> listarSensores() {
        return ponto.getSensores();
    }

    public Sensor buscarSensor(String sensorId) {
        return ponto.buscarSensor(sensorId)
                .orElseThrow(() -> new DadosInvalidosException("Sensor não encontrado no ponto " + ponto.getId() + ": " + sensorId));
    }
    
    public List<SituacaoSensor> verificarSensores() {
        ponto.verificarStatus();
        return ponto.getSensores().stream()
                .map(s -> new SituacaoSensor(s.getId(), s.getTipo(), s.getUnidade(), s.estaFuncionando(),
                        medicaoRepository.countBySensorId(s.getId())))
                .toList();
    }

    public boolean alternarOperacaoSensor(String sensorId) {
        Sensor sensor = buscarSensor(sensorId);
        sensor.definirOperante(!sensor.estaFuncionando());
        ponto.verificarStatus();
        return sensor.estaFuncionando();
    }

    public PontoMonitoramento getPonto() {
        return ponto;
    }

    public Medicao realizarLeitura(String sensorId) {
        Sensor sensor = buscarSensor(sensorId);
        if (!sensor.estaFuncionando()) {
            throw new FonteIndisponivelException("O sensor " + sensor.getId()
                    + " está inoperante; a leitura não foi solicitada. Verifique o sensor (opção 1).");
        }
        double valor = sensor.lerValor();
        return registrarMedicao(sensor, valor);
    }
    
    public Medicao registrarMedicao(Sensor sensor, double valor) {
        Medicao medicao = new Medicao(sensor.getId(), sensor.getTipo(), ponto.getId(), ponto.getRegiao(),
                valor, sensor.getUnidade(), LocalDateTime.now());
        return medicaoRepository.save(medicao);
    }

    public List<Medicao> consultarHistorico(String sensorId) {
        return medicaoRepository.findBySensorIdOrderByDataHoraAscIdAsc(buscarSensor(sensorId).getId());
    }

    public List<Medicao> consultarHistoricoCompleto() {
        return medicaoRepository.findAllByOrderByDataHoraAscIdAsc();
    }

 
    public AnalisePrevisao executarPrevisao(String sensorId, TipoEstrategia tipoEstrategia) {
        Sensor sensor = buscarSensor(sensorId);
        EstrategiaPrevisao estrategia = fabricaPrevisao.criar(tipoEstrategia);

        List<Medicao> historico = medicaoRepository.findBySensorIdOrderByDataHoraDescIdDesc(
                sensor.getId(), Limit.of(estrategia.quantidadeNecessaria()));
        ResultadoPrevisao resultado = estrategia.prever(historico);

        Optional<Alerta> alerta = avaliadorAlertas
                .avaliar(resultado, configuracao.getLimiares(sensor.getTipo()), LocalDateTime.now())
                .map(alertaRepository::save);

        AnalisePrevisao analise = new AnalisePrevisao(resultado, alerta.orElse(null));
        guardarPrevisaoRecente(analise);
        return analise;
    }

    public List<AnalisePrevisao> executarPrevisoes(TipoEstrategia tipoEstrategia) {
        List<AnalisePrevisao> analises = new ArrayList<>();
        for (Sensor sensor : ponto.getSensores()) {
            analises.add(executarPrevisao(sensor.getId(), tipoEstrategia));
        }
        return analises;
    }

    public List<AnalisePrevisao> executarPrevisoesComEstrategiaPadrao() {
        return executarPrevisoes(configuracao.getEstrategiaPadrao());
    }

    public List<Alerta> consultarAlertas() {
        return alertaRepository.findAllByOrderByDataHoraDescIdDesc();
    }

    public List<AnalisePrevisao> previsoesRecentes() {
        return List.copyOf(previsoesRecentes);
    }

    private void guardarPrevisaoRecente(AnalisePrevisao analise) {
        previsoesRecentes.addFirst(analise);
        while (previsoesRecentes.size() > MAXIMO_PREVISOES_RECENTES) {
            previsoesRecentes.removeLast();
        }
    }


    public StatusMonitoramento obterStatus() {
        List<SituacaoSensor> sensores = verificarSensores();
        Map<NivelAlerta, Long> porNivel = new EnumMap<>(NivelAlerta.class);
        for (NivelAlerta nivel : NivelAlerta.values()) {
            porNivel.put(nivel, alertaRepository.countByNivel(nivel));
        }
        List<AnalisePrevisao> recentes = previsoesRecentes();
        return new StatusMonitoramento(
                ponto.getId() + " - " + ponto.getNome(),
                ponto.getStatus(),
                configuracao.getDadosTerritorio().getCodigoRegiao(),
                ponto.getRegiao(),
                sensores,
                medicaoRepository.count(),
                medicaoRepository.findTop5ByOrderByDataHoraDescIdDesc(),
                recentes,
                alertaRepository.count(),
                porNivel,
                descreverSituacaoGeral(ponto.getStatus(), recentes));
    }

    String descreverSituacaoGeral(StatusComponente statusPonto, List<AnalisePrevisao> recentes) {
        StringBuilder situacao = new StringBuilder();
        if (statusPonto == StatusComponente.INOPERANTE) {
            situacao.append("MONITORAMENTO COMPROMETIDO: nenhum sensor está funcionando; novas leituras não são possíveis. ");
        } else if (statusPonto == StatusComponente.DEGRADADO) {
            situacao.append("Monitoramento parcial: há sensor(es) inoperante(s); as previsões desses sensores usam apenas o histórico. ");
        }
        if (recentes.isEmpty()) {
            return situacao.append("Nenhuma previsão foi executada nesta sessão (use a opção 3). ").toString().trim();
        }
        Optional<NivelAlerta> maiorNivel = recentes.stream()
                .map(AnalisePrevisao::alertaEmitido)
                .flatMap(Optional::stream)
                .map(Alerta::getNivel)
                .max(Comparator.naturalOrder());
        long inconclusivas = recentes.stream().filter(a -> !a.resultado().isConclusivo()).count();
        situacao.append(maiorNivel
                .map(n -> "Nível mais grave entre as previsões recentes: " + n.getRotulo() + ". " + n.getRecomendacao() + " ")
                .orElse("Nenhuma previsão recente atingiu os limiares configurados. "));
        if (inconclusivas > 0) {
            situacao.append(inconclusivas).append(" previsão(ões) recente(s) sem histórico suficiente (o sistema não emitiu valor).");
        }
        return situacao.toString().trim();
    }


    public Path exportarHistoricoCsv(String sensorId) throws IOException {
        List<Medicao> medicoes = sensorId == null ? consultarHistoricoCompleto() : consultarHistorico(sensorId);
        return exportadorCSV.exportar(medicoes, sensorId == null ? "todas" : sensorId);
    }
}
