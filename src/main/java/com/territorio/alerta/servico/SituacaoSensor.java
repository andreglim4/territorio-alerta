package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.TipoSensor;

public record SituacaoSensor(String id, TipoSensor tipo, String unidade, boolean funcionando, long medicoesRegistradas) {
}
