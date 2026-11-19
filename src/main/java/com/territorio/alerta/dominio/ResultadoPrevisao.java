package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

public final class ResultadoPrevisao {

    public enum Situacao { HISTORICO_VALIDADO, HISTORICO_INSUFICIENTE, CONCLUSIVA }

    private final String sensorId;
    private final TipoSensor tipoSensor;
    private final String pontoId;
    private final String regiao;
    private final String estrategia;
    private final int quantidadeMedicoes;
    private final Double valorPrevisto;
    private final Tendencia tendencia;
    private final String explicacao;
    private final Situacao situacao;

    private ResultadoPrevisao(String sensorId, TipoSensor tipoSensor, String pontoId, String regiao,
                              String estrategia, int quantidadeMedicoes, Double valorPrevisto,
                              Tendencia tendencia, String explicacao, Situacao situacao) {
        this.sensorId = sensorId;
        this.tipoSensor = tipoSensor;
        this.pontoId = pontoId;
        this.regiao = regiao;
        this.estrategia = estrategia;
        this.quantidadeMedicoes = quantidadeMedicoes;
        this.valorPrevisto = valorPrevisto;
        this.tendencia = tendencia;
        this.explicacao = explicacao;
        this.situacao = situacao;
    }

    static ResultadoPrevisao semHistorico(String estrategia) {
        return new ResultadoPrevisao(null, null, null, null, estrategia, 0, null, Tendencia.INDEFINIDA,
                "Não há nenhuma medição registrada para este sensor. Realize leituras antes de executar previsões.",
                Situacao.HISTORICO_INSUFICIENTE);
    }

    static ResultadoPrevisao insuficiente(Medicao referencia, String estrategia, int quantidade, String explicacao) {
        return new ResultadoPrevisao(referencia.getSensorId(), referencia.getTipoSensor(), referencia.getPontoId(),
                referencia.getRegiao(), estrategia, quantidade, null, Tendencia.INDEFINIDA, explicacao,
                Situacao.HISTORICO_INSUFICIENTE);
    }

    static ResultadoPrevisao historicoValidado(Medicao referencia, String estrategia, int quantidade) {
        return new ResultadoPrevisao(referencia.getSensorId(), referencia.getTipoSensor(), referencia.getPontoId(),
                referencia.getRegiao(), estrategia, quantidade, null, Tendencia.INDEFINIDA,
                "Histórico validado; aguardando o cálculo da estratégia.", Situacao.HISTORICO_VALIDADO);
    }

    public ResultadoPrevisao concluir(double valorPrevisto, int medicoesConsideradas, Tendencia tendencia, String explicacao) {
        if (situacao != Situacao.HISTORICO_VALIDADO) {
            throw new DadosInvalidosException("Só é possível concluir uma previsão cujo histórico foi validado.");
        }
        if (!Double.isFinite(valorPrevisto)) {
            throw new DadosInvalidosException("O valor previsto calculado não é um número válido.");
        }
        return new ResultadoPrevisao(sensorId, tipoSensor, pontoId, regiao, estrategia, medicoesConsideradas,
                valorPrevisto, tendencia, explicacao, Situacao.CONCLUSIVA);
    }

    public boolean isConclusivo() {
        return situacao == Situacao.CONCLUSIVA;
    }

    public boolean isAptoParaCalculo() {
        return situacao == Situacao.HISTORICO_VALIDADO;
    }

    public String getUnidade() {
        return tipoSensor == null ? "" : tipoSensor.getUnidade();
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

    public String getEstrategia() {
        return estrategia;
    }

    public int getQuantidadeMedicoes() {
        return quantidadeMedicoes;
    }

    public Double getValorPrevisto() {
        return valorPrevisto;
    }

    public Tendencia getTendencia() {
        return tendencia;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public Situacao getSituacao() {
        return situacao;
    }
}
