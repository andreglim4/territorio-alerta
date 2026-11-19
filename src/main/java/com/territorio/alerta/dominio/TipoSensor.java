package com.territorio.alerta.dominio;

public enum TipoSensor {

    PLUVIOMETRO("Pluviômetro (intensidade de chuva)", "mm/h", 0.0, 200.0),
    NIVEL_AGUA("Nível de água", "cm", 0.0, 300.0),
    UMIDADE_SOLO("Umidade/saturação do solo", "%", 0.0, 100.0);

    private final String descricao;
    private final String unidade;
    private final double valorMinimo;
    private final double valorMaximo;

    TipoSensor(String descricao, String unidade, double valorMinimo, double valorMaximo) {
        this.descricao = descricao;
        this.unidade = unidade;
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
    }

    public boolean aceita(double valor) {
        return Double.isFinite(valor) && valor >= valorMinimo && valor <= valorMaximo;
    }

    public double limitarAFaixa(double valor) {
        return Math.max(valorMinimo, Math.min(valorMaximo, valor));
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }

    public double getValorMinimo() {
        return valorMinimo;
    }

    public double getValorMaximo() {
        return valorMaximo;
    }
}
