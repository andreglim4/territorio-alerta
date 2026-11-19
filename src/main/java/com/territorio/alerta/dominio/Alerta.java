package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta")
public class Alerta {

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

    @Column(name = "valor_previsto", nullable = false)
    private double valorPrevisto;

    @Column(name = "limite_ultrapassado", nullable = false)
    private double limiteUltrapassado;

    @Column(name = "unidade", nullable = false, length = 10)
    private String unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 10)
    private NivelAlerta nivel;

    @Column(name = "estrategia", nullable = false, length = 60)
    private String estrategia;

    @Column(name = "explicacao", nullable = false, length = 1000)
    private String explicacao;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected Alerta() {
    }

    public Alerta(String sensorId, TipoSensor tipoSensor, String pontoId, String regiao,
                  double valorPrevisto, double limiteUltrapassado, NivelAlerta nivel,
                  String estrategia, String explicacao, LocalDateTime dataHora) {
        if (tipoSensor == null || nivel == null || dataHora == null) {
            throw new DadosInvalidosException("Tipo do sensor, nível e data do alerta são obrigatórios.");
        }
        if (!Double.isFinite(valorPrevisto) || valorPrevisto < 0 || !Double.isFinite(limiteUltrapassado) || limiteUltrapassado < 0) {
            throw new DadosInvalidosException("Valor previsto e limite do alerta devem ser números não negativos.");
        }
        if (valorPrevisto < limiteUltrapassado) {
            throw new DadosInvalidosException("Um alerta só pode ser emitido quando o valor previsto atinge o limite.");
        }
        this.sensorId = exigirTexto(sensorId, "sensor");
        this.tipoSensor = tipoSensor;
        this.pontoId = exigirTexto(pontoId, "ponto monitorado");
        this.regiao = exigirTexto(regiao, "região");
        this.valorPrevisto = valorPrevisto;
        this.limiteUltrapassado = limiteUltrapassado;
        this.unidade = tipoSensor.getUnidade();
        this.nivel = nivel;
        this.estrategia = exigirTexto(estrategia, "estratégia");
        this.explicacao = exigirTexto(explicacao, "explicação");
        this.dataHora = dataHora;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo '" + campo + "' do alerta é obrigatório.");
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

    public double getValorPrevisto() {
        return valorPrevisto;
    }

    public double getLimiteUltrapassado() {
        return limiteUltrapassado;
    }

    public String getUnidade() {
        return unidade;
    }

    public NivelAlerta getNivel() {
        return nivel;
    }

    public String getEstrategia() {
        return estrategia;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
