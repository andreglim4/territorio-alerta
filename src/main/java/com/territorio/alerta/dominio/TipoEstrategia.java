package com.territorio.alerta.dominio;

public enum TipoEstrategia {

    MEDIA_MOVEL("Média móvel", "média das últimas N medições do sensor"),
    COMPARACAO("Comparação entre períodos", "compara a média do período recente com a do período anterior e projeta a tendência");

    private final String nome;
    private final String descricao;

    TipoEstrategia(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }
}
