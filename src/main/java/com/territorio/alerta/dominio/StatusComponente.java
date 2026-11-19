package com.territorio.alerta.dominio;

public enum StatusComponente {

    NAO_VERIFICADO("Ainda não verificado"),
    OPERACIONAL("Operacional: todos os sensores funcionando"),
    DEGRADADO("Degradado: parte dos sensores está inoperante"),
    INOPERANTE("Inoperante: nenhum sensor está funcionando"),
    SEM_SENSORES("Sem sensores associados");

    private final String descricao;

    StatusComponente(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
