package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.ResultadoPrevisao;

import java.util.Optional;

public record AnalisePrevisao(ResultadoPrevisao resultado, Alerta alerta) {

    public Optional<Alerta> alertaEmitido() {
        return Optional.ofNullable(alerta);
    }
}
