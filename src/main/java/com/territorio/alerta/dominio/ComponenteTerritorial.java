package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

public abstract class ComponenteTerritorial {

    private String id;
    private String nome;
    private String regiao;
    private StatusComponente status;

    protected ComponenteTerritorial(String id, String nome, String regiao) {
        definirId(id);
        setNome(nome);
        setRegiao(regiao);
        this.status = StatusComponente.NAO_VERIFICADO;
    }

    public abstract StatusComponente verificarStatus();

    public String descrever() {
        return String.format("%s - %s | Região: %s | Status: %s", id, nome, regiao, status.getDescricao());
    }

    protected final void atualizarStatus(StatusComponente novoStatus) {
        if (novoStatus == null) {
            throw new DadosInvalidosException("O status de um componente territorial não pode ser nulo.");
        }
        this.status = novoStatus;
    }

    protected static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo '" + campo + "' do componente territorial é obrigatório.");
        }
        return valor.trim();
    }

    private void definirId(String id) {
        this.id = exigirTexto(id, "id");
    }

    public final void setNome(String nome) {
        this.nome = exigirTexto(nome, "nome");
    }

    public final void setRegiao(String regiao) {
        this.regiao = exigirTexto(regiao, "região");
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegiao() {
        return regiao;
    }

    public StatusComponente getStatus() {
        return status;
    }
}
