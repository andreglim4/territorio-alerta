package com.territorio.alerta.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatos {

    public static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Formatos() {
    }

    public static String numero(double valor) {
        return String.format(PT_BR, "%.1f", valor);
    }

    public static String valor(double valor, String unidade) {
        return numero(valor) + " " + unidade;
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "-" : DATA_HORA.format(dataHora);
    }
}
