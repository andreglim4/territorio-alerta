package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DadosTerritorio {

    private String codigoRegiao;
    private String nomeRegiao;
    private int populacaoEstimada;
    private double areaKm2;
    private int pontosVulneraveis;
    private final Map<TipoSensor, Limiares> limiares = new EnumMap<>(TipoSensor.class);

    public DadosTerritorio(String codigoRegiao, String nomeRegiao, int populacaoEstimada,
                           double areaKm2, int pontosVulneraveis) {
        setCodigoRegiao(codigoRegiao);
        setNomeRegiao(nomeRegiao);
        setPopulacaoEstimada(populacaoEstimada);
        setAreaKm2(areaKm2);
        setPontosVulneraveis(pontosVulneraveis);
    }

    public void definirLimiares(TipoSensor tipo, double atencao, double alerta, double critico) {
        if (tipo == null) {
            throw new DadosInvalidosException("Informe o tipo de sensor ao qual os limiares se aplicam.");
        }
        Limiares novos = new Limiares(atencao, alerta, critico);
        if (critico > tipo.getValorMaximo()) {
            throw new DadosInvalidosException(String.format(
                    "O limiar crítico de %.1f %s está acima do valor máximo que o sensor de %s consegue medir (%.1f %s).",
                    critico, tipo.getUnidade(), tipo.getDescricao(), tipo.getValorMaximo(), tipo.getUnidade()));
        }
        limiares.put(tipo, novos);
    }

    public Limiares getLimiares(TipoSensor tipo) {
        Limiares encontrados = limiares.get(tipo);
        if (encontrados == null) {
            throw new DadosInvalidosException("Não há limiares configurados para o tipo de sensor " + tipo + ".");
        }
        return encontrados;
    }

    public Map<TipoSensor, Limiares> getTodosLimiares() {
        return Collections.unmodifiableMap(new EnumMap<>(limiares));
    }

    public void setCodigoRegiao(String codigoRegiao) {
        if (codigoRegiao == null || codigoRegiao.isBlank()) {
            throw new DadosInvalidosException("O código da região é obrigatório.");
        }
        this.codigoRegiao = codigoRegiao.trim();
    }

    public void setNomeRegiao(String nomeRegiao) {
        if (nomeRegiao == null || nomeRegiao.isBlank()) {
            throw new DadosInvalidosException("O nome da região é obrigatório.");
        }
        this.nomeRegiao = nomeRegiao.trim();
    }

    public void setPopulacaoEstimada(int populacaoEstimada) {
        if (populacaoEstimada < 0) {
            throw new DadosInvalidosException("A população estimada não pode ser negativa (informado: " + populacaoEstimada + ").");
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public void setAreaKm2(double areaKm2) {
        if (!Double.isFinite(areaKm2) || areaKm2 <= 0) {
            throw new DadosInvalidosException("A área da região deve ser um número maior que zero (informado: " + areaKm2 + ").");
        }
        this.areaKm2 = areaKm2;
    }

    public void setPontosVulneraveis(int pontosVulneraveis) {
        if (pontosVulneraveis < 0) {
            throw new DadosInvalidosException("A quantidade de pontos vulneráveis não pode ser negativa (informado: " + pontosVulneraveis + ").");
        }
        this.pontosVulneraveis = pontosVulneraveis;
    }

    public String getCodigoRegiao() {
        return codigoRegiao;
    }

    public String getNomeRegiao() {
        return nomeRegiao;
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    public double getAreaKm2() {
        return areaKm2;
    }

    public int getPontosVulneraveis() {
        return pontosVulneraveis;
    }
}
