package com.territorio.alerta.dominio;

public enum NivelAlerta {

    ATENCAO("ATENÇÃO", "de atenção",
            "Intensificar o acompanhamento da região e verificar bueiros, galerias e sistemas de drenagem."),
    ALERTA("ALERTA", "de alerta",
            "Preparar equipes para possíveis ocorrências e avaliar a comunicação preventiva à população."),
    CRITICO("CRÍTICO", "crítico",
            "Realizar avaliação preventiva imediata da região, priorizar recursos e avaliar o acionamento da Defesa Civil.");

    private final String rotulo;
    private final String nomeDoLimite;
    private final String recomendacao;

    NivelAlerta(String rotulo, String nomeDoLimite, String recomendacao) {
        this.rotulo = rotulo;
        this.nomeDoLimite = nomeDoLimite;
        this.recomendacao = recomendacao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getNomeDoLimite() {
        return nomeDoLimite;
    }

    public String getRecomendacao() {
        return recomendacao;
    }
}
