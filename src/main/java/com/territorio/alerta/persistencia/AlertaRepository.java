package com.territorio.alerta.persistencia;

import com.territorio.alerta.dominio.Alerta;
import com.territorio.alerta.dominio.NivelAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    List<Alerta> findAllByOrderByDataHoraDescIdDesc();

    List<Alerta> findBySensorIdOrderByDataHoraDescIdDesc(String sensorId);

    long countByNivel(NivelAlerta nivel);
}
