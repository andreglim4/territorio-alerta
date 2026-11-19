package com.territorio.alerta.persistencia;

import com.territorio.alerta.dominio.Medicao;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicaoRepository extends JpaRepository<Medicao, Long> {

    List<Medicao> findBySensorIdOrderByDataHoraAscIdAsc(String sensorId);

    List<Medicao> findAllByOrderByDataHoraAscIdAsc();

    List<Medicao> findBySensorIdOrderByDataHoraDescIdDesc(String sensorId, Limit limite);

    List<Medicao> findTop5ByOrderByDataHoraDescIdDesc();

    long countBySensorId(String sensorId);
}
