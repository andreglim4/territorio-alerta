package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.EstrategiaPrevisao;
import com.territorio.alerta.dominio.PrevisaoComparacao;
import com.territorio.alerta.dominio.PrevisaoMediaMovel;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.excecao.DadosInvalidosException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FabricaPrevisao {

    private final ConfiguracaoSistema configuracao;
    private final Map<TipoEstrategia, Function<ConfiguracaoSistema, EstrategiaPrevisao>> criadores =
            new EnumMap<>(TipoEstrategia.class);

    public FabricaPrevisao(ConfiguracaoSistema configuracao) {
        if (configuracao == null) {
            throw new DadosInvalidosException("A fábrica de previsões precisa da configuração do sistema.");
        }
        this.configuracao = configuracao;
        criadores.put(TipoEstrategia.MEDIA_MOVEL, cfg -> new PrevisaoMediaMovel(cfg.getJanelaMediaMovel()));
        criadores.put(TipoEstrategia.COMPARACAO, cfg -> new PrevisaoComparacao(cfg.getTamanhoPeriodoComparacao()));
    }

    public EstrategiaPrevisao criar(TipoEstrategia tipo) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe qual estratégia de previsão deve ser criada.");
        }
        Function<ConfiguracaoSistema, EstrategiaPrevisao> criador = criadores.get(tipo);
        if (criador == null) {
            throw new DadosInvalidosException("Estratégia de previsão não suportada: " + tipo + ".");
        }
        return criador.apply(configuracao);
    }

    public EstrategiaPrevisao criarPadrao() {
        return criar(configuracao.getEstrategiaPadrao());
    }

    public Set<TipoEstrategia> tiposDisponiveis() {
        return Collections.unmodifiableSet(criadores.keySet());
    }
}
