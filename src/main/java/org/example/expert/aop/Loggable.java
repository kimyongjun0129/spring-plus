package org.example.expert.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 어노테이션은 메소드 실행 결과에 대한 로그를 기록하는 역할을 합니다.
 * <p>
 * 메소드 실행 후 성공 또는 실패에 대한 로그를 남기기 위해 사용됩니다.
 * <ul>
 *     <li>성공 메시지와 실패 메시지는 기본값으로 각각 "성공"과 "실패"가 제공됩니다.</li>
 *     <li>필요에 따라 성공 및 실패 메시지를 커스터마이즈할 수 있습니다.</li>
 * </ul>
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * 메소드 실행 성공 시 로그에 기록될 메시지.
     * 기본값은 "성공"이며, 필요에 따라 커스터마이즈 가능합니다.
     *
     * @return 성공 메시지
     */
    String successMessage() default "성공";

    /**
     * 메소드 실행 실패 시 로그에 기록될 메시지.
     * 기본값은 "실패"이며, 필요에 따라 커스터마이즈 가능합니다.
     *
     * @return 실패 메시지
     */
    String failureMessage() default "실패";
}

