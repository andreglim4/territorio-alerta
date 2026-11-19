package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;
import com.territorio.alerta.excecao.FonteIndisponivelException;

import java.util.Random;

public abstract class SensorSimulado implements Sensor {

    private static final double VALOR_ESPURIO = -1.0;

    private final String id;
    private final TipoSensor tipo;
    private final double probabilidadeFalha;
    private final double probabilidadeLeituraEspuria;
    private boolean operante = true;

    protected final Random random;

    protected SensorSimulado(String id, TipoSensor tipo, Random random,
                             double probabilidadeFalha, double probabilidadeLeituraEspuria) {
        if (id == null || id.isBlank()) {
            throw new DadosInvalidosException("Todo sensor precisa de um identificador.");
        }
        if (tipo == null || random == null) {
            throw new DadosInvalidosException("Tipo e gerador de leituras do sensor " + id + " são obrigatórios.");
        }
        validarProbabilidade(probabilidadeFalha, "falha");
        validarProbabilidade(probabilidadeLeituraEspuria, "leitura espúria");
        this.id = id.trim();
        this.tipo = tipo;
        this.random = random;
        this.probabilidadeFalha = probabilidadeFalha;
        this.probabilidadeLeituraEspuria = probabilidadeLeituraEspuria;
    }

    @Override
    public final double lerValor() {
        if (!operante) {
            throw new FonteIndisponivelException("O sensor " + id + " (" + tipo.getDescricao()
                    + ") está inoperante (em manutenção ou desligado). Nenhuma leitura foi obtida.");
        }
        if (random.nextDouble() < probabilidadeFalha) {
            throw new FonteIndisponivelException("Falha de comunicação simulada com o sensor " + id
                    + " (" + tipo.getDescricao() + "): a leitura não chegou ao sistema.");
        }
        if (random.nextDouble() < probabilidadeLeituraEspuria) {
            return VALOR_ESPURIO;
        }
        return arredondar(tipo.limitarAFaixa(simularProximaLeitura()));
    }

    protected abstract double simularProximaLeitura();

    @Override
    public boolean estaFuncionando() {
        return operante;
    }

    @Override
    public void definirOperante(boolean operante) {
        this.operante = operante;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TipoSensor getTipo() {
        return tipo;
    }

    protected static double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private static void validarProbabilidade(double probabilidade, String descricao) {
        if (!Double.isFinite(probabilidade) || probabilidade < 0.0 || probabilidade > 1.0) {
            throw new DadosInvalidosException("A probabilidade de " + descricao + " deve estar entre 0 e 1.");
        }
    }

    @Override
    public String toString() {
        return id + " - " + tipo.getDescricao() + " (" + getUnidade() + ")";
    }
}
