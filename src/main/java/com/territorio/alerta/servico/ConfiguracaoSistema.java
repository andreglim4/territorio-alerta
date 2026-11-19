package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.DadosTerritorio;
import com.territorio.alerta.dominio.Limiares;
import com.territorio.alerta.dominio.PrevisaoComparacao;
import com.territorio.alerta.dominio.PrevisaoMediaMovel;
import com.territorio.alerta.dominio.TipoEstrategia;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;

public final class ConfiguracaoSistema {

    public static final int JANELA_MAXIMA = 50;
    public static final int PERIODO_MAXIMO = 25;

    private DadosTerritorio dadosTerritorio;
    private TipoEstrategia estrategiaPadrao;
    private int janelaMediaMovel;
    private int tamanhoPeriodoComparacao;

    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    private static final class Portador {
        private static final ConfiguracaoSistema INSTANCIA = new ConfiguracaoSistema();
    }

    public static ConfiguracaoSistema getInstancia() {
        return Portador.INSTANCIA;
    }

    public synchronized void restaurarPadroes() {
        DadosTerritorio territorio = new DadosTerritorio(
                "REG-01", "Região Baixada do Córrego (fictícia)", 12000, 3.5, 4);
        territorio.definirLimiares(TipoSensor.PLUVIOMETRO, 10.0, 25.0, 50.0);  // mm/h
        territorio.definirLimiares(TipoSensor.NIVEL_AGUA, 30.0, 50.0, 80.0);   // cm
        territorio.definirLimiares(TipoSensor.UMIDADE_SOLO, 70.0, 85.0, 95.0); // %
        this.dadosTerritorio = territorio;
        this.estrategiaPadrao = TipoEstrategia.MEDIA_MOVEL;
        this.janelaMediaMovel = 5;
        this.tamanhoPeriodoComparacao = 3;
    }

    public synchronized Limiares getLimiares(TipoSensor tipo) {
        return dadosTerritorio.getLimiares(tipo);
    }

    public synchronized void alterarLimiares(TipoSensor tipo, double atencao, double alerta, double critico) {
        dadosTerritorio.definirLimiares(tipo, atencao, alerta, critico);
    }

    public synchronized DadosTerritorio getDadosTerritorio() {
        return dadosTerritorio;
    }

    public synchronized TipoEstrategia getEstrategiaPadrao() {
        return estrategiaPadrao;
    }

    public synchronized void setEstrategiaPadrao(TipoEstrategia estrategiaPadrao) {
        if (estrategiaPadrao == null) {
            throw new DadosInvalidosException("Informe uma estratégia de previsão válida.");
        }
        this.estrategiaPadrao = estrategiaPadrao;
    }

    public synchronized int getJanelaMediaMovel() {
        return janelaMediaMovel;
    }

    public synchronized void setJanelaMediaMovel(int janela) {
        if (janela < PrevisaoMediaMovel.JANELA_MINIMA || janela > JANELA_MAXIMA) {
            throw new DadosInvalidosException(String.format(
                    "A janela da média móvel deve estar entre %d e %d medições (informado: %d).",
                    PrevisaoMediaMovel.JANELA_MINIMA, JANELA_MAXIMA, janela));
        }
        this.janelaMediaMovel = janela;
    }

    public synchronized int getTamanhoPeriodoComparacao() {
        return tamanhoPeriodoComparacao;
    }

    public synchronized void setTamanhoPeriodoComparacao(int tamanho) {
        if (tamanho < PrevisaoComparacao.PERIODO_MINIMO || tamanho > PERIODO_MAXIMO) {
            throw new DadosInvalidosException(String.format(
                    "Cada período da comparação deve ter entre %d e %d medições (informado: %d).",
                    PrevisaoComparacao.PERIODO_MINIMO, PERIODO_MAXIMO, tamanho));
        }
        this.tamanhoPeriodoComparacao = tamanho;
    }
}
