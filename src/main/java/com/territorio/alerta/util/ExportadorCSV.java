package com.territorio.alerta.util;

import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.excecao.DadosInvalidosException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ExportadorCSV {

    static final String SEPARADOR = ";";
    static final String CABECALHO = String.join(SEPARADOR,
            "id_medicao", "id_sensor", "tipo_sensor", "ponto_monitorado", "regiao", "valor", "unidade", "data_hora");
    private static final char BOM_UTF8 = '﻿';
    private static final DateTimeFormatter CARIMBO_ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path diretorio;

    public ExportadorCSV(@Value("${territorio.exportacao.diretorio:exportacoes}") String diretorio) {
        this.diretorio = Paths.get(diretorio);
    }

    public Path exportar(List<Medicao> medicoes, String identificacao) throws IOException {
        if (medicoes == null || medicoes.isEmpty()) {
            throw new DadosInvalidosException("Não há medições para exportar: realize leituras antes de gerar o CSV.");
        }
        Files.createDirectories(diretorio);
        String nomeSeguro = identificacao == null ? "medicoes" : identificacao.replaceAll("[^A-Za-z0-9_-]", "_");
        Path arquivo = diretorio.resolve("medicoes_" + nomeSeguro + "_" + CARIMBO_ARQUIVO.format(LocalDateTime.now()) + ".csv");

        try (BufferedWriter escritor = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            escritor.write(BOM_UTF8);
            escritor.write(CABECALHO);
            escritor.newLine();
            for (Medicao medicao : medicoes) {
                escritor.write(linha(medicao));
                escritor.newLine();
            }
        }
        return arquivo.toAbsolutePath();
    }

    static String linha(Medicao m) {
        return String.join(SEPARADOR,
                escapar(m.getId() == null ? "" : String.valueOf(m.getId())),
                escapar(m.getSensorId()),
                escapar(m.getTipoSensor().getDescricao()),
                escapar(m.getPontoId()),
                escapar(m.getRegiao()),
                escapar(Formatos.numero(m.getValor())),
                escapar(m.getUnidade()),
                escapar(Formatos.dataHora(m.getDataHora())));
    }

    static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        boolean precisaAspas = campo.contains(SEPARADOR) || campo.contains("\"")
                || campo.contains("\n") || campo.contains("\r");
        if (!precisaAspas) {
            return campo;
        }
        return "\"" + campo.replace("\"", "\"\"") + "\"";
    }
}
