package com.territorio.alerta.util;

import com.territorio.alerta.MedicoesDeTeste;
import com.territorio.alerta.dominio.Medicao;
import com.territorio.alerta.dominio.TipoSensor;
import com.territorio.alerta.excecao.DadosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportadorCSVTest {

    @TempDir
    Path pastaTemporaria;

    @Test
    @DisplayName("CSV tem BOM UTF-8, cabeçalho e uma linha por medição com valor, unidade e data")
    void deveExportarMedicoes() throws IOException {
        ExportadorCSV exportador = new ExportadorCSV(pastaTemporaria.toString());
        List<Medicao> medicoes = MedicoesDeTeste.sequencia("PLU-01", TipoSensor.PLUVIOMETRO, 12.5, 30.0);

        Path arquivo = exportador.exportar(medicoes, "PLU-01");

        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        assertEquals(3, linhas.size());
        assertEquals('﻿' + ExportadorCSV.CABECALHO, linhas.get(0));
        assertTrue(linhas.get(1).contains(";PLU-01;"));
        assertTrue(linhas.get(1).contains(";12,5;mm/h;"));
        assertTrue(linhas.get(2).contains(";30,0;mm/h;"));
        assertTrue(linhas.get(1).matches(".*;\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}$"));
    }

    @Test
    @DisplayName("Campos com separador ou aspas são escapados conforme RFC 4180")
    void deveEscaparCamposEspeciais() {
        Medicao medicao = new Medicao("NIV-01", TipoSensor.NIVEL_AGUA, "PM-01", "Bairro \"Baixo\"; setor 2",
                40.0, "cm", LocalDateTime.now().minusMinutes(1));

        String linha = ExportadorCSV.linha(medicao);

        assertTrue(linha.contains(";\"Bairro \"\"Baixo\"\"; setor 2\";"), linha);
        assertEquals("texto simples", ExportadorCSV.escapar("texto simples"));
    }

    @Test
    @DisplayName("Exportar histórico vazio é recusado com mensagem clara")
    void deveRecusarHistoricoVazio() {
        ExportadorCSV exportador = new ExportadorCSV(pastaTemporaria.toString());

        assertThrows(DadosInvalidosException.class, () -> exportador.exportar(List.of(), "todas"));
    }
}
