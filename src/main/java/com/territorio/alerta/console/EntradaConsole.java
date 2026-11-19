package com.territorio.alerta.console;

import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class EntradaConsole {

    public static class EntradaEncerradaException extends RuntimeException {
        public EntradaEncerradaException() {
            super("A entrada do console foi encerrada.");
        }
    }

    private final Scanner scanner = new Scanner(System.in);

    public String lerTexto(String pergunta) {
        System.out.print(pergunta);
        if (!scanner.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        return scanner.nextLine().replace("﻿", "").trim();
    }

    public int lerInteiro(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("  Entrada inválida: '" + texto + "' não é um número inteiro. Tente novamente.");
            }
        }
    }

    public int lerInteiro(String pergunta, int minimo, int maximo) {
        while (true) {
            int valor = lerInteiro(pergunta);
            if (valor >= minimo && valor <= maximo) {
                return valor;
            }
            System.out.printf("  Opção inexistente: digite um número entre %d e %d.%n", minimo, maximo);
        }
    }

    public double lerDecimal(String pergunta) {
        while (true) {
            String texto = lerTexto(pergunta);
            try {
                return Double.parseDouble(texto.replace(',', '.'));
            } catch (NumberFormatException e) {
                System.out.println("  Entrada inválida: '" + texto + "' não é um número. Use, por exemplo, 12,5.");
            }
        }
    }

    public void aguardarEnter() {
        lerTexto("\nPressione ENTER para voltar ao menu...");
    }
}
