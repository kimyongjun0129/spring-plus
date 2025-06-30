package org.example.expert.domain.log.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.aop.Loggable;
import org.example.expert.domain.log.entity.Log;
import org.example.expert.domain.log.repository.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void managerRegisterSuccess(Loggable loggable, LocalDateTime now) {
        logRepository.save(new Log(loggable.successMessage(), now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void managerRegisterFail(Loggable loggable, Throwable ex, LocalDateTime now) {
        logRepository.save(new Log(loggable.failureMessage() + ": " + ex.getMessage(), now));
    }
}
