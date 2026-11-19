package com.territorio.alerta.console;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.ResultadoPrevisao;
import com.territorio.alerta.servico.AnalisePrevisao;
import com.territorio.alerta.util.Formatos;

import java.util.List;

final class Tela {

    private static final int LARGURA = 100;

    private Tela() {
    }

    static void titulo(String texto) {
        System.out.println();
        System.out.println("-".repeat(LARGURA));
        System.out.println(" " + texto);
        System.out.println("-".repeat(LARGURA));
    }

    static void sucesso(String texto) {
        System.out.println("  [OK] " + texto);
    }

    static void aviso(String texto) {
        paragrafo("  [AVISO] ", texto);
    }

    static void erro(String texto) {
        paragrafo("  [ERRO] ", texto);
    }

    static void paragrafo(String prefixo, String texto) {
        String recuo = " ".repeat(prefixo.length());
        StringBuilder linha = new StringBuilder(prefixo);
        for (String palavra : texto.split("\\s+")) {
            if (linha.length() + palavra.length() + 1 > LARGURA && linha.length() > prefixo.length()) {
                System.out.println(linha.toString().stripTrailing());
                linha = new StringBuilder(recuo);
            }
            linha.append(palavra).append(' ');
        }
        System.out.println(linha.toString().stripTrailing());
    }

    static void tabelaMedicoes(List<Medicao> medicoes) {
        if (medicoes.isEmpty()) {
            System.out.println("  Nenhuma medição registrada.");
            return;
        }
        System.out.printf("  %-6s %-8s %-36s %10s %-6s %s%n", "ID", "SENSOR", "TIPO", "VALOR", "UNID.", "DATA/HORA");
        for (Medicao m : medicoes) {
            System.out.printf("  %-6d %-8s %-36s %10s %-6s %s%n", m.getId(), m.getSensorId(),
                    m.getTipoSensor().getDescricao(), Formatos.numero(m.getValor()), m.getUnidade(),
                    Formatos.dataHora(m.getDataHora()));
        }
    }

    static void alerta(Alerta a) {
        System.out.printf("  #%d | %s | %-8s | sensor %s | previsto %s | limite %s | estratégia: %s%n",
                a.getId(), Formatos.dataHora(a.getDataHora()), a.getNivel().getRotulo(), a.getSensorId(),
                Formatos.valor(a.getValorPrevisto(), a.getUnidade()), Formatos.valor(a.getLimiteUltrapassado(), a.getUnidade()),
                a.getEstrategia());
        paragrafo("     ", a.getExplicacao());
        System.out.println();
    }

    static void analise(AnalisePrevisao analise) {
        ResultadoPrevisao r = analise.resultado();
        System.out.println();
        System.out.println("  Sensor analisado .....: " + (r.getSensorId() == null ? "-" : r.getSensorId()
                + " - " + r.getTipoSensor().getDescricao()));
        System.out.println("  Região monitorada ....: " + (r.getRegiao() == null ? "-" : r.getRegiao()));
        System.out.println("  Estratégia ...........: " + r.getEstrategia());
        System.out.println("  Medições consideradas : " + r.getQuantidadeMedicoes());
        System.out.println("  Valor previsto .......: " + (r.isConclusivo()
                ? Formatos.valor(r.getValorPrevisto(), r.getUnidade())
                : "NÃO EMITIDO (histórico insuficiente)"));
        System.out.println("  Tendência ............: " + r.getTendencia().getDescricao());
        paragrafo("  Explicação ...........: ", r.getExplicacao());
        analise.alertaEmitido().ifPresentOrElse(
                a -> paragrafo(" ALERTA REGISTRADO (#" + a.getId() + "): ", a.getExplicacao()),
                () -> System.out.println(r.isConclusivo()
                        ? " Sem alerta: o valor previsto não atingiu nenhum limiar configurado."
                        : " Sem alerta: sem previsão fundamentada, o sistema não emite alerta."));
    }
}
