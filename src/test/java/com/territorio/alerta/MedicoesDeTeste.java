package com.territorio.alerta;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.TipoSensor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class MedicoesDeTeste {

    private MedicoesDeTeste() {
    }

    public static List<Medicao> sequencia(String sensorId, TipoSensor tipo, double... valores) {
        LocalDateTime inicio = LocalDateTime.now().minusHours(2);
        List<Medicao> medicoes = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            medicoes.add(new Medicao(sensorId, tipo, "PM-01", "Região de Teste", valores[i], tipo.getUnidade(),
                    inicio.plusMinutes(i)));
        }
        return medicoes;
    }
}
