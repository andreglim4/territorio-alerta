package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.FonteIndisponivelException;

public interface Sensor {

    String getId();

    TipoSensor getTipo();

    double lerValor();

    boolean estaFuncionando();

    void definirOperante(boolean operante);

    default String getUnidade() {
        return getTipo().getUnidade();
    }
}
