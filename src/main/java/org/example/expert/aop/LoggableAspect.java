package org.example.expert.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.log.service.LogService;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggableAspect {

    private final LogService logService;

    @Around("@annotation(loggable)")
    public Object logExecution(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        LocalDateTime now = LocalDateTime.now();

        // 기본값 -1은 로그에 미리 "Unknown"으로 표시
        long id = -1L;
        long managerUserId = -1L;
        long todoId = -1L;

        // 메소드 인자 처리: 해당 메소드에서 전달받은 인자들을 꺼내서 필요한 값을 대입합니다.
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof ManagerSaveRequest managerSaveRequest) {
                managerUserId = managerSaveRequest.getManagerUserId(); // 매니저 ID
            } else if (arg instanceof AuthUser authUser) {
                id = authUser.getId(); // 요청한 유저의 ID
            } else if (arg instanceof Long todo) {
                todoId = todo; // Todo ID
            }
        }

        // 로그 메시지 설정: 성공 및 실패 메시지는 어노테이션에서 전달된 값으로 설정됩니다.
        String successMessage = loggable.successMessage();
        String failureMessage = loggable.failureMessage();

        // 함수 실행 후 성공 로그 처리
        try {
            Object result = joinPoint.proceed(); // 메소드 실행
            logService.managerRegisterSuccess(loggable, now); // 성공 시 로그 저장
            log.info("성공: {} - 요청 유저={}, 등록 매니저 ID={}, Todo ID={}, time={}",
                    successMessage,
                    id == -1 ? "Unknown" : id,
                    managerUserId == -1 ? "Unknown" : managerUserId,
                    todoId == -1 ? "Unknown" : todoId,
                    now
            );
            return result;
        } catch (Throwable ex) {
            // 예외가 발생하면 실패 로그 저장
            logService.managerRegisterFail(loggable, ex, now);
            log.error("실패: {} - 요청 유저={}, 등록 매니저 ID={}, Todo ID={}, time={}",
                    failureMessage,
                    id == -1 ? "Unknown" : id,
                    managerUserId == -1 ? "Unknown" : managerUserId,
                    todoId == -1 ? "Unknown" : todoId,
                    now
            );
            throw ex; // 예외를 다시 던져서 트랜잭션 처리가 정상적으로 이루어지도록 함
        }
    }
}
