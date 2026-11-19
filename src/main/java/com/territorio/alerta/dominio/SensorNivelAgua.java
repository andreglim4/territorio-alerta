package com.territorio.alerta.dominio;

import java.util.Random;

public class SensorNivelAgua extends SensorSimulado {

    private static final double NIVEL_MAXIMO_SIMULADO = 250.0;

    private double nivelAtual;

    public SensorNivelAgua(String id) {
        this(id, new Random(), 0.05, 0.02);
    }

    public SensorNivelAgua(String id, Random random, double probabilidadeFalha, double probabilidadeLeituraEspuria) {
        super(id, TipoSensor.NIVEL_AGUA, random, probabilidadeFalha, probabilidadeLeituraEspuria);
        this.nivelAtual = 10.0 + random.nextDouble() * 20.0;
    }

    @Override
    protected double simularProximaLeitura() {
        if (random.nextDouble() < 0.15) {
            nivelAtual -= 5.0 + random.nextDouble() * 10.0;
        } else {
            nivelAtual += 2.0 + random.nextGaussian() * 3.0;
        }
        nivelAtual = limitar(nivelAtual, 0.0, NIVEL_MAXIMO_SIMULADO);
        return nivelAtual;
    }
}
