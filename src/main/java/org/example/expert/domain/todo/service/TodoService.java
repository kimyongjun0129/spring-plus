package org.example.expert.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

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

    public Page<TodoSearchResponse> getTodosSearch(int page, int size, String title, LocalDateTime start, LocalDateTime end, String managerName) {

        Pageable pageable = PageRequest.of(page - 1, size);

        return todoRepository.fetchScheduleTitlesWithCounts(title, managerName, start, end, pageable);
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }
}
