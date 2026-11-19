package com.territorio.alerta.dominio;

import java.util.Random;


public class PluviometroSensor extends SensorSimulado {

    private static final double INTENSIDADE_MAXIMA_SIMULADA = 150.0;

    private double intensidadeAtual;

    public PluviometroSensor(String id) {
        this(id, new Random(), 0.05, 0.02);
    }

    public PluviometroSensor(String id, Random random, double probabilidadeFalha, double probabilidadeLeituraEspuria) {
        super(id, TipoSensor.PLUVIOMETRO, random, probabilidadeFalha, probabilidadeLeituraEspuria);
        this.intensidadeAtual = random.nextDouble() * 15.0;
    }

    @Override
    protected double simularProximaLeitura() {
        double sorteio = random.nextDouble();
        if (sorteio < 0.10) {
            intensidadeAtual += 15.0 + random.nextDouble() * 25.0;
        } else if (sorteio < 0.20) {
            intensidadeAtual *= 0.3;
        } else {
            intensidadeAtual += random.nextGaussian() * 4.0 + 1.0;
        }
        intensidadeAtual = limitar(intensidadeAtual, 0.0, INTENSIDADE_MAXIMA_SIMULADA);
        return intensidadeAtual;
    }
}
