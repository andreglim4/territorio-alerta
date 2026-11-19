package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.util.Formatos;

import java.util.List;
import java.util.stream.Collectors;

public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    public static final int JANELA_MINIMA = 5;

    private final int janela;

    public PrevisaoMediaMovel(int janela) {
        super(TipoEstrategia.MEDIA_MOVEL.getNome());
        if (janela < JANELA_MINIMA) {
            throw new DadosInvalidosException("A janela da média móvel deve ter pelo menos " + JANELA_MINIMA + " medições.");
        }
        this.janela = janela;
    }

    @Override
    public int quantidadeNecessaria() {
        return janela;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao base = super.prever(historico);
        if (!base.isAptoParaCalculo()) {
            return base;
        }

        List<Medicao> consideradas = ultimasMedicoes(historico, janela);
        double mediaMovel = media(consideradas);
        double primeira = consideradas.get(0).getValor();
        double ultima = consideradas.get(consideradas.size() - 1).getValor();
        double variacaoNaJanela = ultima - primeira;
        Tendencia tendencia = Tendencia.aPartirDaVariacao(variacaoNaJanela, primeira);
        String unidade = base.getUnidade();

        String valores = consideradas.stream()
                .map(m -> Formatos.numero(m.getValor()))
                .collect(Collectors.joining("; "));
        String explicacao = String.format(
                "Média das últimas %d medições (%s) = %s. A média móvel suaviza oscilações e indica o patamar recente: "
                        + "se o comportamento se mantiver, espera-se um valor próximo de %s. "
                        + "Da primeira para a última medição considerada houve variação de %s %s (%s).",
                consideradas.size(), valores, Formatos.valor(mediaMovel, unidade), Formatos.valor(mediaMovel, unidade),
                variacaoNaJanela >= 0 ? "+" + Formatos.numero(variacaoNaJanela) : Formatos.numero(variacaoNaJanela),
                unidade, tendencia.getDescricao());

        return base.concluir(mediaMovel, consideradas.size(), tendencia, explicacao);
    }

    public int getJanela() {
        return janela;
    }
}
