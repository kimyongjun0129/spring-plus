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

        long id = -1L;
        long managerUserId = -1L;
        long todoId = -1L;

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof ManagerSaveRequest managerSaveRequest) {
                managerUserId = managerSaveRequest.getManagerUserId();
            } else if (arg instanceof AuthUser authUser) {
                id = authUser.getId();
            } else if (arg instanceof Long todo) {
                todoId = todo;
            }
        }

        try {
            Object result = joinPoint.proceed();
            logService.managerRegisterSuccess(loggable, now);
            return result;
        } catch (Throwable ex) {
            logService.managerRegisterFail(loggable, ex, now);
            throw ex;
        } finally {
            log.info("요청 유저={}, 등록 매니저 ID={}, Todo ID={}, time={}",
                    id==-1?"Unknown":id,
                    managerUserId==-1?"Unknown":managerUserId,
                    todoId==-1?"Unknown":todoId,
                    now
            );
        }
    }
}