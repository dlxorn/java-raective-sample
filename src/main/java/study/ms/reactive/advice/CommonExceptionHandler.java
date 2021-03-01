package study.ms.reactive.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class CommonExceptionHandler {

  //일반 스프링에서는 throw를 try-catch로 잡아서 처리하는 방법이 있는데
  //리액티브에서는 그렇게 불가능할 듯 싶다.
  //실행시점이 다르니,
  //이것처럼 함수형태에서는 에러를 잡아서 처리하는 게 불가능한가?

  //아래처럼 핸들러로 잡는 방식은 리액티브에서는 적합하지 않을 듯 싶다.
  //에러 핸들링 방법부터 찾아야겠다.
  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(value = Exception.class)
  public Mono<String> errorHandler(ServerWebExchange exchange, Exception e) {
    return Mono.just(e.toString());
  }

}
