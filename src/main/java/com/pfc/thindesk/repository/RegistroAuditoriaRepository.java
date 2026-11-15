package com.pfc.thindesk.repository;

import com.pfc.thindesk.entity.RegistroAuditoria;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegistroAuditoriaRepository extends MongoRepository<RegistroAuditoria, String> {
    List<RegistroAuditoria> findAllByOrderByQuandoDesc();
}
