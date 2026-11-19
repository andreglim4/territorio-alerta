package com.territorio.alerta.dominio;

import com.territorio.alerta.excecao.DadosInvalidosException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PontoMonitoramento extends ComponenteTerritorial {

    private final List<Sensor> sensores = new ArrayList<>();

    public PontoMonitoramento(String id, String nome, String regiao) {
        super(id, nome, regiao);
    }

    public void adicionarSensor(Sensor sensor) {
        if (sensor == null) {
            throw new DadosInvalidosException("Não é possível associar um sensor nulo ao ponto " + getId() + ".");
        }
        if (buscarSensor(sensor.getId()).isPresent()) {
            throw new DadosInvalidosException("Já existe um sensor com o id " + sensor.getId() + " no ponto " + getId() + ".");
        }
        sensores.add(sensor);
    }

    public Optional<Sensor> buscarSensor(String sensorId) {
        if (sensorId == null) {
            return Optional.empty();
        }
        return sensores.stream()
                .filter(sensor -> sensor.getId().equalsIgnoreCase(sensorId.trim()))
                .findFirst();
    }

    public List<Sensor> getSensores() {
        return List.copyOf(sensores);
    }

    @Override
    public StatusComponente verificarStatus() {
        long funcionando = sensores.stream().filter(Sensor::estaFuncionando).count();
        StatusComponente novoStatus;
        if (sensores.isEmpty()) {
            novoStatus = StatusComponente.SEM_SENSORES;
        } else if (funcionando == sensores.size()) {
            novoStatus = StatusComponente.OPERACIONAL;
        } else if (funcionando == 0) {
            novoStatus = StatusComponente.INOPERANTE;
        } else {
            novoStatus = StatusComponente.DEGRADADO;
        }
        atualizarStatus(novoStatus);
        return novoStatus;
    }
}
