package com.territorio.alerta.servico;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.Limiares;
import com.territorio.alerta.dominio.NivelAlerta;
import com.territorio.alerta.dominio.ResultadoPrevisao;
import com.territorio.alerta.util.Formatos;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class AvaliadorAlertas {

    public Optional<Alerta> avaliar(ResultadoPrevisao resultado, Limiares limiares, LocalDateTime momento) {
        if (resultado == null || !resultado.isConclusivo()) {
            return Optional.empty();
        }
        double valorPrevisto = resultado.getValorPrevisto();
        return limiares.classificar(valorPrevisto)
                .map(nivel -> criarAlerta(resultado, nivel, limiares.limiteDo(nivel), momento));
    }

    private Alerta criarAlerta(ResultadoPrevisao resultado, NivelAlerta nivel, double limite, LocalDateTime momento) {
        return new Alerta(resultado.getSensorId(), resultado.getTipoSensor(), resultado.getPontoId(),
                resultado.getRegiao(), resultado.getValorPrevisto(), limite, nivel, resultado.getEstrategia(),
                explicar(resultado, nivel, limite), momento);
    }

    String explicar(ResultadoPrevisao resultado, NivelAlerta nivel, double limite) {
        String unidade = resultado.getUnidade();
        return String.format(
                "%s: o sensor %s [%s] do ponto %s, localizado em %s, apresentou PREVISÃO de %s pela estratégia '%s', "
                        + "igual ou acima do limite %s configurado de %s. %s "
                        + "Trata-se de uma previsão baseada em medições simuladas, e não da confirmação de que um alagamento ocorreu. "
                        + "Recomendação para avaliação da equipe: %s",
                nivel.getRotulo(), resultado.getSensorId(), resultado.getTipoSensor().getDescricao(),
                resultado.getPontoId(), resultado.getRegiao(),
                Formatos.valor(resultado.getValorPrevisto(), unidade), resultado.getEstrategia(),
                nivel.getNomeDoLimite(), Formatos.valor(limite, unidade),
                "Observou-se " + resultado.getTendencia().getDescricao() + ".",
                nivel.getRecomendacao());
    }
}
