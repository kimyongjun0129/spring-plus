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

    /**
     * 관리자 등록 성공 시 로그를 기록하는 메서드입니다.
     *
     * - `loggable`: 로그를 기록할 객체로, 성공 메시지를 제공하는 인터페이스입니다.
     * - `now`: 현재 시간으로, 로그 기록 시점을 나타냅니다.
     *
     * 이 메서드는 `REQUIRES_NEW` 전파 속성을 사용하여 새로운 트랜잭션을 생성하고,
     * 로그를 데이터베이스에 저장합니다.
     * 성공적인 관리자 등록을 기록합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void managerRegisterSuccess(Loggable loggable, LocalDateTime now) {
        logRepository.save(new Log(loggable.successMessage(), now));
    }

    /**
     * 관리자 등록 실패 시 로그를 기록하는 메서드입니다.
     *
     * - `loggable`: 로그를 기록할 객체로, 실패 메시지를 제공하는 인터페이스입니다.
     * - `ex`: 예외 객체로, 실패 원인에 대한 메시지를 제공합니다.
     * - `now`: 현재 시간으로, 로그 기록 시점을 나타냅니다.
     *
     * 이 메서드는 `REQUIRES_NEW` 전파 속성을 사용하여 새로운 트랜잭션을 생성하고,
     * 로그를 데이터베이스에 저장합니다.
     * 관리자 등록 실패와 관련된 정보를 기록합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void managerRegisterFail(Loggable loggable, Throwable ex, LocalDateTime now) {
        logRepository.save(new Log(loggable.failureMessage() + ": " + ex.getMessage(), now));
    }

}
