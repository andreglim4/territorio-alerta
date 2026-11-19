package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.StatusComponente;

import java.util.List;
import java.util.Map;

public record StatusMonitoramento(
        String pontoDescricao,
        StatusComponente statusPonto,
        String codigoRegiao,
        String nomeRegiao,
        List<SituacaoSensor> sensores,
        long totalMedicoes,
        List<Medicao> ultimasMedicoes,
        List<AnalisePrevisao> ultimasPrevisoes,
        long totalAlertas,
        Map<NivelAlerta, Long> alertasPorNivel,
        String situacaoGeral) {
}
