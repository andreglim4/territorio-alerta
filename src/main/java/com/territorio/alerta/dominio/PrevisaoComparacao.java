package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.util.Formatos;

import java.util.List;

public class PrevisaoComparacao extends EstrategiaPrevisao {

    public static final int PERIODO_MINIMO = 3;

    private final int tamanhoPeriodo;

    public PrevisaoComparacao(int tamanhoPeriodo) {
        super(TipoEstrategia.COMPARACAO.getNome());
        if (tamanhoPeriodo < PERIODO_MINIMO) {
            throw new DadosInvalidosException("Cada período da comparação deve ter pelo menos " + PERIODO_MINIMO + " medições.");
        }
        this.tamanhoPeriodo = tamanhoPeriodo;
    }

    @Override
    public int quantidadeNecessaria() {
        return tamanhoPeriodo * 2;
    }

    @Override
    public ResultadoPrevisao prever(List<Medicao> historico) {
        ResultadoPrevisao base = super.prever(historico);
        if (!base.isAptoParaCalculo()) {
            return base;
        }

        List<Medicao> consideradas = ultimasMedicoes(historico, quantidadeNecessaria());
        double mediaAnterior = media(consideradas.subList(0, tamanhoPeriodo));
        double mediaRecente = media(consideradas.subList(tamanhoPeriodo, consideradas.size()));
        double variacao = mediaRecente - mediaAnterior;
        double projecao = mediaRecente + variacao;

        TipoSensor tipo = base.getTipoSensor();
        double previsao = tipo.limitarAFaixa(projecao);
        Tendencia tendencia = Tendencia.aPartirDaVariacao(variacao, mediaAnterior);
        String unidade = base.getUnidade();

        StringBuilder explicacao = new StringBuilder(String.format(
                "Média do período anterior (%d medições) = %s; média do período recente (%d medições) = %s. "
                        + "Variação entre os períodos: %s %s (%s). Mantida essa tendência, o próximo período tende a %s.",
                tamanhoPeriodo, Formatos.valor(mediaAnterior, unidade), tamanhoPeriodo, Formatos.valor(mediaRecente, unidade),
                variacao >= 0 ? "+" + Formatos.numero(variacao) : Formatos.numero(variacao), unidade,
                tendencia.getDescricao(), Formatos.valor(previsao, unidade)));
        if (previsao != projecao) {
            explicacao.append(String.format(" (A projeção matemática de %s foi ajustada para respeitar a faixa física do sensor.)",
                    Formatos.valor(projecao, unidade)));
        }

        return base.concluir(previsao, consideradas.size(), tendencia, explicacao.toString());
    }

    public int getTamanhoPeriodo() {
        return tamanhoPeriodo;
    }
}
