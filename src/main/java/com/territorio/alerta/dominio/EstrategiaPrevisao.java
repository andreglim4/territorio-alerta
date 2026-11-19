package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class EstrategiaPrevisao {

    private final String nome;

    protected EstrategiaPrevisao(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("Toda estratégia de previsão precisa de um nome.");
        }
        this.nome = nome;
    }

    public abstract int quantidadeNecessaria();

    public ResultadoPrevisao prever(List<Medicao> historico) {
        if (historico == null) {
            throw new DadosInvalidosException("O histórico de medições não pode ser nulo.");
        }
        if (historico.isEmpty()) {
            return ResultadoPrevisao.semHistorico(nome);
        }
        Medicao referencia = historico.get(0);
        if (referencia == null) {
            throw new DadosInvalidosException("O histórico contém uma medição nula.");
        }
        for (Medicao medicao : historico) {
            validarCompatibilidade(referencia, medicao);
        }
        if (historico.size() < quantidadeNecessaria()) {
            return ResultadoPrevisao.insuficiente(referencia, nome, historico.size(), String.format(
                    "Histórico insuficiente: há %d medição(ões) do sensor %s, mas a estratégia '%s' exige pelo menos %d. "
                            + "Para não apresentar um número sem fundamento, nenhuma previsão foi emitida.",
                    historico.size(), referencia.getSensorId(), nome, quantidadeNecessaria()));
        }
        return ResultadoPrevisao.historicoValidado(referencia, nome, historico.size());
    }

    private void validarCompatibilidade(Medicao referencia, Medicao medicao) {
        if (medicao == null) {
            throw new DadosInvalidosException("O histórico contém uma medição nula.");
        }
        if (!medicao.getSensorId().equals(referencia.getSensorId())) {
            throw new DadosInvalidosException(String.format(
                    "O histórico mistura medições dos sensores %s e %s; a previsão deve usar um único sensor.",
                    referencia.getSensorId(), medicao.getSensorId()));
        }
        if (medicao.getTipoSensor() != referencia.getTipoSensor() || !medicao.getUnidade().equals(referencia.getUnidade())) {
            throw new DadosInvalidosException(String.format(
                    "O histórico do sensor %s contém unidades incompatíveis (%s e %s).",
                    referencia.getSensorId(), referencia.getUnidade(), medicao.getUnidade()));
        }
    }

    protected List<Medicao> ultimasMedicoes(List<Medicao> historico, int quantidade) {
        List<Medicao> ordenadas = new ArrayList<>(historico);
        ordenadas.sort(Comparator.comparing(Medicao::getDataHora));
        return ordenadas.subList(Math.max(0, ordenadas.size() - quantidade), ordenadas.size());
    }

    protected static double media(List<Medicao> medicoes) {
        return medicoes.stream().mapToDouble(Medicao::getValor).average()
                .orElseThrow(() -> new DadosInvalidosException("Não é possível calcular a média de uma lista vazia."));
    }

    public String getNome() {
        return nome;
    }
}
