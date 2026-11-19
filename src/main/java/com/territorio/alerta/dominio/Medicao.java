package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicao", indexes = @Index(name = "idx_medicao_sensor_data", columnList = "sensor_id, data_hora"))
public class Medicao {

    private static final int TOLERANCIA_RELOGIO_SEGUNDOS = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_id", nullable = false, length = 20)
    private String sensorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sensor", nullable = false, length = 20)
    private TipoSensor tipoSensor;

    @Column(name = "ponto_id", nullable = false, length = 20)
    private String pontoId;

    @Column(name = "regiao", nullable = false, length = 120)
    private String regiao;

    @Column(name = "valor", nullable = false)
    private double valor;

    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected Medicao() {
    }

    public Medicao(String sensorId, TipoSensor tipoSensor, String pontoId, String regiao,
                   double valor, String unidade, LocalDateTime dataHora) {
        setSensorId(sensorId);
        setTipoSensor(tipoSensor);
        setPontoId(pontoId);
        setRegiao(regiao);
        setUnidade(unidade);
        setValor(valor);
        setDataHora(dataHora);
    }

    private void setSensorId(String sensorId) {
        this.sensorId = exigirTexto(sensorId, "identificação do sensor");
    }

    private void setTipoSensor(TipoSensor tipoSensor) {
        if (tipoSensor == null) {
            throw new DadosInvalidosException("O tipo do sensor da medição é obrigatório.");
        }
        this.tipoSensor = tipoSensor;
    }

    private void setPontoId(String pontoId) {
        this.pontoId = exigirTexto(pontoId, "identificação do ponto monitorado");
    }

    private void setRegiao(String regiao) {
        this.regiao = exigirTexto(regiao, "região");
    }

    private void setUnidade(String unidade) {
        String informada = exigirTexto(unidade, "unidade");
        if (!informada.equals(tipoSensor.getUnidade())) {
            throw new DadosInvalidosException(String.format(
                    "Unidade incompatível para o sensor %s: recebido '%s', esperado '%s' (%s).",
                    sensorId, informada, tipoSensor.getUnidade(), tipoSensor.getDescricao()));
        }
        this.unidade = informada;
    }

    private void setValor(double valor) {
        if (!Double.isFinite(valor)) {
            throw new DadosInvalidosException("Medição do sensor " + sensorId + " não é um número válido.");
        }
        if (valor < 0) {
            throw new DadosInvalidosException(String.format(
                    "Medição negativa rejeitada: o sensor %s (%s) informou %.1f %s; esse tipo de medição não pode ser negativo.",
                    sensorId, tipoSensor.getDescricao(), valor, tipoSensor.getUnidade()));
        }
        if (!tipoSensor.aceita(valor)) {
            throw new DadosInvalidosException(String.format(
                    "Medição fora da faixa aceita: o sensor %s (%s) informou %.1f %s, mas a faixa válida é de %.1f a %.1f %s.",
                    sensorId, tipoSensor.getDescricao(), valor, tipoSensor.getUnidade(),
                    tipoSensor.getValorMinimo(), tipoSensor.getValorMaximo(), tipoSensor.getUnidade()));
        }
        this.valor = valor;
    }

    private void setDataHora(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new DadosInvalidosException("A data e hora da medição são obrigatórias.");
        }
        if (dataHora.isAfter(LocalDateTime.now().plusSeconds(TOLERANCIA_RELOGIO_SEGUNDOS))) {
            throw new DadosInvalidosException("A medição do sensor " + sensorId + " tem data no futuro (" + dataHora + ").");
        }
        this.dataHora = dataHora;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo '" + campo + "' da medição é obrigatório.");
        }
        return valor.trim();
    }

    public Long getId() {
        return id;
    }

    public String getSensorId() {
        return sensorId;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public String getPontoId() {
        return pontoId;
    }

    public String getRegiao() {
        return regiao;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    @Override
    public String toString() {
        return String.format("Medicao[id=%s, sensor=%s, valor=%.1f %s, dataHora=%s]", id, sensorId, valor, unidade, dataHora);
    }
}
