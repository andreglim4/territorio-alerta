package com.territorio.alerta.dominio;

public enum Tendencia {

    AUMENTO("tendência de AUMENTO"),
    REDUCAO("tendência de REDUÇÃO"),
    ESTABILIDADE("tendência de ESTABILIDADE"),
    INDEFINIDA("tendência indefinida (dados insuficientes)");

    private static final double TOLERANCIA_RELATIVA = 0.05;
    private static final double TOLERANCIA_ABSOLUTA_MINIMA = 0.5;

    private final String descricao;

    Tendencia(String descricao) {
        this.descricao = descricao;
    }

    public static Tendencia aPartirDaVariacao(double variacao, double referencia) {
        double tolerancia = Math.max(TOLERANCIA_ABSOLUTA_MINIMA, Math.abs(referencia) * TOLERANCIA_RELATIVA);
        if (variacao > tolerancia) {
            return AUMENTO;
        }
        if (variacao < -tolerancia) {
            return REDUCAO;
        }
        return ESTABILIDADE;
    }

    public String getDescricao() {
        return descricao;
    }
}
