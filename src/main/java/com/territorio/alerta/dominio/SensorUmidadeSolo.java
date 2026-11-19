package com.territorio.alerta.dominio;

import java.util.Random;

public class SensorUmidadeSolo extends SensorSimulado {

    private double umidadeAtual;

    public SensorUmidadeSolo(String id) {
        this(id, new Random(), 0.05, 0.02);
    }

    public SensorUmidadeSolo(String id, Random random, double probabilidadeFalha, double probabilidadeLeituraEspuria) {
        super(id, TipoSensor.UMIDADE_SOLO, random, probabilidadeFalha, probabilidadeLeituraEspuria);
        this.umidadeAtual = 45.0 + random.nextDouble() * 20.0;
    }

    @Override
    protected double simularProximaLeitura() {
        double espacoAteSaturar = 100.0 - umidadeAtual;
        umidadeAtual += espacoAteSaturar * (0.02 + random.nextDouble() * 0.08);
        if (random.nextDouble() < 0.15) {
            umidadeAtual -= random.nextDouble() * 6.0;
        }
        umidadeAtual = limitar(umidadeAtual, 0.0, 100.0);
        return umidadeAtual;
    }
}
