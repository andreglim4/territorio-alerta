package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

import java.util.Optional;

public final class Limiares {

    private final double atencao;
    private final double alerta;
    private final double critico;

    public Limiares(double atencao, double alerta, double critico) {
        validarValor(atencao, "atenção");
        validarValor(alerta, "alerta");
        validarValor(critico, "crítico");
        if (!(atencao < alerta && alerta < critico)) {
            throw new DadosInvalidosException(String.format(
                    "Limiares inconsistentes (atenção=%.1f, alerta=%.1f, crítico=%.1f): "
                            + "é obrigatório que atenção < alerta < crítico.", atencao, alerta, critico));
        }
        this.atencao = atencao;
        this.alerta = alerta;
        this.critico = critico;
    }

    public Optional<NivelAlerta> classificar(double valorPrevisto) {
        if (valorPrevisto >= critico) {
            return Optional.of(NivelAlerta.CRITICO);
        }
        if (valorPrevisto >= alerta) {
            return Optional.of(NivelAlerta.ALERTA);
        }
        if (valorPrevisto >= atencao) {
            return Optional.of(NivelAlerta.ATENCAO);
        }
        return Optional.empty();
    }

    public double limiteDo(NivelAlerta nivel) {
        return switch (nivel) {
            case ATENCAO -> atencao;
            case ALERTA -> alerta;
            case CRITICO -> critico;
        };
    }

    private static void validarValor(double valor, String nome) {
        if (!Double.isFinite(valor)) {
            throw new DadosInvalidosException("O limiar de " + nome + " precisa ser um número válido.");
        }
        if (valor < 0) {
            throw new DadosInvalidosException("O limiar de " + nome + " não pode ser negativo (informado: " + valor + ").");
        }
    }

    public double getAtencao() {
        return atencao;
    }

    public double getAlerta() {
        return alerta;
    }

    public double getCritico() {
        return critico;
    }
}
