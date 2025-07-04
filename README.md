# SPRING PLUS

----
### 1. 코드 개선 퀴즈 - @Transactional의 이해
#### 문제 : 다음 오류를 해결하시오.
```text
jakarta.servlet.ServletException: 
Request processing failed: org.springframework.orm.jpa.JpaSystemException: 
could not execute statement 
[Connection is read-only. Queries leading to data modification are not allowed]
[insert into todos (contents,created_at,modified_at,title,user_id,weather) values (?,?,?,?,?,?)]
```
#### 해결 :
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {}
```
* `@Transaction(readOnly = true)` 이므로 이 서비스 로직 내에서는 삽입, 수정, 삭제를 할 수 없습니다.
* 따라서 `@Transaction(readOnly = true)`를 삭제해야 합니다.
---
### 2. 코드 추가 퀴즈 - JWT의 이해
#### 문제 : 다음 요구사항을 만족하시오.
```text
- User의 정보에 nickname이 필요해졌어요.
    - User 테이블에 nickname 컬럼을 추가해주세요.
    - nickname은 중복 가능합니다.
- 프론트엔드 개발자가 JWT에서 유저의 닉네임을 꺼내 화면에 보여주길 원하고 있어요.
```
#### 해결 :
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String nickname;
    @NotBlank
    private String userRole;
}
```
* 회원 가입할 때, `nickname` 값을 추가로 받습니다.

```java
 public SigninResponse signin(SigninRequest signinRequest) {
        // 검증 로직

        String bearerToken = jwtUtil.createToken(user.getId(), user.getEmail(), user.getNickname(), user.getUserRole(), user.getNickname());

        return new SigninResponse(bearerToken);
    }
```
* 로그인 할 때, 토큰에 nickname 값도 넣어줍니다.
---
### 3. 코드 개선 퀴즈 - JPA의 이해
#### 문제 : 다음 요구사항을 만족하시오.
```text
- 할 일 검색 시 `weather` 조건으로도 검색할 수 있어야해요.
    - `weather` 조건은 있을 수도 있고, 없을 수도 있어요!
- 할 일 검색 시 수정일 기준으로 기간 검색이 가능해야해요.
    - 기간의 시작과 끝 조건은 있을 수도 있고, 없을 수도 있어요!
- JPQL을 사용하고, 쿼리 메소드명은 자유롭게 지정하되 너무 길지 않게 해주세요.
```
해결 :
```java
    @GetMapping("/todos")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(required = false) String weather
            ) {
        return ResponseEntity.ok(todoService.getTodos(page, size, weather, start, end));
    }
```
```java
    public Page<TodoResponse> getTodos(int page, int size, String weather, LocalDateTime start, LocalDateTime end) {
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<Todo> todos;

        // weather, start, end 조건이 다 있는 경우
        if (weather != null && start != null && end != null) {
            todos = todoRepository.findTodosByWeatherAndDateRange(weather, start, end, pageable);
            return getTodoResponses(todos);
        }

        // weather 조건만 있는 경우
        if (weather != null) {
            todos = todoRepository.findTodosByWeatherOrderedDesc(weather, pageable);
            return getTodoResponses(todos);
        }

        // 기간의 시작과 끝 조건 이 있는 경우
        if (start != null && end != null) {
            todos = todoRepository.findTodosByModifiedDateBetween(start, end, pageable);
            return getTodoResponses(todos);
        }

        // 아무 조건도 없는 경우
        todos = todoRepository.findAllByOrderByModifiedAtDesc(pageable);
        return getTodoResponses(todos);
    }

    private static Page<TodoResponse> getTodoResponses(Page<Todo> todos) {
        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }
```
* 파라미터 값이 NULL 인지에 따라 다른 JPQL 적용
```java
public interface TodoRepository extends JpaRepository<Todo, Long>, TodoDslRepository{

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u WHERE t.weather = :weather ORDER BY t.modifiedAt DESC")
    Page<Todo> findTodosByWeatherOrderedDesc(String weather, Pageable pageable);

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u WHERE t.modifiedAt > :start AND t.modifiedAt < :end ORDER BY t.modifiedAt DESC")
    Page<Todo> findTodosByModifiedDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u WHERE t.weather = :weather AND t.modifiedAt > :start AND t.modifiedAt < :end ORDER BY t.modifiedAt DESC")
    Page<Todo> findTodosByWeatherAndDateRange(String weather, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
```
* 개선 사항 : 위에서처럼 4번의 JPQL 사용하지말고, `IS NULL` 을 사용하면 한 번의 JPQL로도 나타낼 수 있습니다.
---
### 4. 테스트 코드 퀴즈 - 컨트롤러 테스트의 이해
#### 문제 : 잘못된 테스트 코드 수정하기
```text
테스트 패키지 org.example.expert.domain.todo.controller의 
todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() 테스트가 실패하고 있어요.
```
#### 해결 : 
```java
    @Test
    void todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long todoId = 1L;

        // when
        when(todoService.getTodo(todoId))
                .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }
```
* 상태 코드가 잘못 설정되어 있어서 다시 고쳐서 해결하였습니다.
---
### 5. 코드 개선 퀴즈 - AOP의 이해
#### 문제 : 잘못된 AOP 수정
```text
- `UserAdminController` 클래스의 `changeUserRole()` 메소드가 실행 전 동작해야해요.
- `AdminAccessLoggingAspect` 클래스에 있는 AOP가 개발 의도에 맞도록 코드를 수정해주세요.
```
#### 해결 :
```java
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAccessLoggingAspect {

    private final HttpServletRequest request;

    @Before("execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
    public void logAfterChangeUserRole(JoinPoint joinPoint) {
        // 로직 수행
    }
}
```
* 메소드 실행 전에 시작하기 위해서 `@After`를 `@Before`로 변경하였습니다.
---
### 6. JPA Cascade
#### 문제 : JPA casecade 옵션 누락
```text
- 할 일을 새로 저장할 시, 할 일을 생성한 유저는 담당자로 자동 등록되어야 합니다.
- JPA의 `cascade` 기능을 활용해 할 일을 생성한 유저가 담당자로 등록될 수 있게 해주세요.
```
#### 해결 :
```java
@Getter
@Entity
@NoArgsConstructor
@Table(name = "todos")
public class Todo extends Timestamped {

    // 다른 컬럼들

    @OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
    private List<Manager> managers = new ArrayList<>();

    // 생성자
}
```
* cascade 옵션인 `PERSIST`를 사용하여, 해당 요구사항대로 동작시킬 수 있습니다.
---
### 7. N+1
#### 문제 : N+1 문제 해결
```text
- `CommentController` 클래스의 `getComments()` API를 호출할 때 N+1 문제가 발생하고 있어요. N+1 문제란, 데이터베이스 쿼리 성능 저하를 일으키는 대표적인 문제 중 하나로, 특히 연관된 엔티티를 조회할 때 발생해요.
- 해당 문제가 발생하지 않도록 코드를 수정해주세요.
```
#### 해결 : 
```java
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN fetch c.user WHERE c.todo.id = :todoId")
    List<Comment> findByTodoIdWithUser(@Param("todoId") Long todoId);
}
```
* 기존에 `JOIN`에서 `JOIN FETCH` 전략으로 변경하여 Comment가 조회될 때, User도 바로 조회될 수 있게 변경하였습니다.
---
### 8. QueryDSL
#### 문제 : 기존에 JPQL을 QueryDSL로 변경
```text
- JPQL로 작성된 `findByIdWithUser` 를 QueryDSL로 변경합니다.
- 7번과 마찬가지로 N+1 문제가 발생하지 않도록 유의해 주세요!
```

#### 해결 :
```java
@Repository
@RequiredArgsConstructor
public class TodoDslRepositoryImpl implements  TodoDslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        QTodo todo = QTodo.todo;
        return Optional.ofNullable(jpaQueryFactory.selectFrom(todo)
                .leftJoin(todo.user, user).fetchJoin()
                .where(todo.id.eq(todoId))
                .fetchOne());
    }
}
```

---
### 9. Spring Security
#### 문제 : Spring Security 적용
```text
- 기존 `Filter`와 `Argument Resolver`를 사용하던 코드들을 Spring Security로 변경해주세요.
    - 접근 권한 및 유저 권한 기능은 그대로 유지해주세요.
    - 권한은 Spring Security의 기능을 사용해주세요.
- 토큰 기반 인증 방식은 유지할 거예요. JWT는 그대로 사용해주세요.
```
#### 해결 :
```java
@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth (로그인, 회원가입)
                        .requestMatchers(HttpMethod.POST, "/auth/*").permitAll()

                        // Comment (댓글 생성 및 조회)
                        .requestMatchers(HttpMethod.POST, "/todos/*/comments").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos/*/comments").permitAll()

                        // Manager (매니저 생성, 조회 및 삭제)
                        .requestMatchers(HttpMethod.POST, "/todos/*/managers").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos/*/managers").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/todos/*/managers/*").hasRole(UserRole.USER.name())

                        // To do (To do 생성 및 조회)
                        .requestMatchers(HttpMethod.POST, "/todos").hasRole(UserRole.USER.name())
                        .requestMatchers(HttpMethod.GET, "/todos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/todos/*").permitAll()

                        // User (User 조회 및 수정)
                        .requestMatchers(HttpMethod.GET, "/users/*").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users").hasRole(UserRole.USER.name())

                        // Admin (User 역할 변경)
                        .requestMatchers(HttpMethod.PATCH, "/admin/users/*").hasRole(UserRole.ADMIN.name())
                        .anyRequest().hasRole(UserRole.ADMIN.name())
                )
                .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
```
* 기존에 있던 resolver와 resolver를 등록하기 위한 configure를 삭제해야 오류가 발생하지 않습니다.
```java
    @PostMapping("/todos")
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        // todo 로직
    }
```
* 인증된 유저의 정보를 `@AuthenticationPrinciapl`을 통해서 전달합니다.
---
### 10. QueryDSL 을 사용하여 검색 기능 만들기
#### 문제 : 
```text
👉 일정을 검색하는 기능을 만들고 싶어요!
검색 기능의 성능 및 사용성을 높이기 위해 QueryDSL을 활용한 쿼리 최적화를 해보세요.
❗Projections를 활용해서 필요한 필드만 반환할 수 있도록 해주세요❗

- 새로운 API로 만들어주세요.
- 검색 조건은 다음과 같아요.
    - 검색 키워드로 일정의 제목을 검색할 수 있어요.
        - 제목은 부분적으로 일치해도 검색이 가능해요.
    - 일정의 생성일 범위로 검색할 수 있어요.
        - 일정을 생성일 최신순으로 정렬해주세요.
    - 담당자의 닉네임으로도 검색이 가능해요.
        - 닉네임은 부분적으로 일치해도 검색이 가능해요.
- 다음의 내용을 포함해서 검색 결과를 반환해주세요.
    - 일정에 대한 모든 정보가 아닌, 제목만 넣어주세요.
    - 해당 일정의 담당자 수를 넣어주세요.
    - 해당 일정의 총 댓글 개수를 넣어주세요.
- 검색 결과는 페이징 처리되어 반환되도록 합니다.
```
#### 해결 :
```java
@Repository
@RequiredArgsConstructor
public class TodoDslRepositoryImpl implements  TodoDslRepository {

    private final JPAQueryFactory jpaQueryFactory;
    
    @Override
    public Page<TodoSearchResponse> fetchScheduleTitlesWithCounts(String title, String managerNickname, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        QTodo todo = QTodo.todo;
        QManager manager = QManager.manager;
        QComment comment = QComment.comment;

        // 메인 쿼리
        List<TodoSearchResponse> results = jpaQueryFactory
                .select(
                        Projections.constructor(
                                TodoSearchResponse.class,
                                todo.title,
                                manager.id.countDistinct(),
                                comment.id.countDistinct()
                        )
                )
                .from(todo)
                .leftJoin(todo.managers, manager)
                .leftJoin(todo.comments, comment)
                .where(
                        titleContains(title),
                        createdAtBetween(start, end),
                        managerNicknameContains(managerNickname)
                )
                .groupBy(todo.id)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // totalCount 계산
        Long total = jpaQueryFactory
                .select(todo.countDistinct())
                .from(todo)
                .leftJoin(todo.managers)
                .where(
                        titleContains(title),
                        createdAtBetween(start, end),
                        managerNicknameContains(managerNickname)
                )
                .fetchOne();

        return new PageImpl<>(results, pageable, total == null ? 0 : total);
    }


    private BooleanExpression titleContains(String title) {
        return StringUtils.hasText(title) ? QTodo.todo.title.containsIgnoreCase(title) : null;
    }

    private BooleanExpression createdAtBetween(LocalDateTime start, LocalDateTime end) {
        return (start != null && end != null) ? QTodo.todo.createdAt.between(start, end) : null;
    }

    private BooleanExpression managerNicknameContains(String nickname) {
        return StringUtils.hasText(nickname) ? todo.managers.any().managerNickname.containsIgnoreCase(nickname) : null;
    }
}
```
---
### 11. Transaction 심화
#### 문제 : 
```text
👉 매니저 등록 요청 시 로그를 남기고 싶어요!
`@Transactional`의 옵션 중 하나를 활용하여 매니저 등록과 로그 기록이 각각 독립적으로 처리될 수 있도록 해봅시다.


- 매니저 등록 요청을 기록하는 로그 테이블을 만들어주세요.
    - DB 테이블명: `log`
- 매니저 등록과는 별개로 로그 테이블에는 항상 요청 로그가 남아야 해요.
    - 매니저 등록은 실패할 수 있지만, 로그는 반드시 저장되어야 합니다.
    - 로그 생성 시간은 반드시 필요합니다.
    - 그 외 로그에 들어가는 내용은 원하는 정보를 자유롭게 넣어주세요.
```
#### 해결 :
```java
@Entity
@Table(name = "log")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private LocalDateTime createdAt;

    public Log(String message, LocalDateTime createdAt) {
        this.message = message;
        this.createdAt = createdAt;
    }

    protected Log() {

    }
}
```
* 테이블에 저장될 로그 Entity
```java
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
```
* 로그를 저장하고 출력하는 AOP
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    String successMessage() default "성공";
    String failureMessage() default "실패";
}
```
* Method가 어떤 기능을 성공 혹은 실패했는지 확인하기 위해 message를 Custom 할 수 있고 전달할 수 있는 어노테이션
```java
@Service
@RequiredArgsConstructor
public class ManagerService {

    // 의존성 주입 객체

    @Transactional
    @Loggable(successMessage = "매니저 추가 성공", failureMessage = "매니저 추가 실패")
    public ManagerSaveResponse saveManager(AuthUser authUser, long todoId, ManagerSaveRequest managerSaveRequest) {

        // 서비스 로직
    }
}
```
* `@Loggable` 어노테이션을 통해 `매니저 추가 성공`, `매니저 추가 실패`와 같은 메시지로 Custom 할 수 있습니다.
```java
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
```
* 성공하든 실패하든 Log는 저장되어야 합니다. 이를 위해 Transaction 전파 옵션인 `Propagation.REQUIRES_NEW` 옵션을 통해 상위 Transaction 에서 Rollback이 일어나도 진행되게끔 처리하였습니다.