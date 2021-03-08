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
  //리액티브에서는 모노로 만들어진 것들은, 모노가 실행되는 시점이 아니면,
  //실행이 안되기 때문에 모노 로직을 try-catch로 처리하는 방식은 안될 것 같다.

  //그러나 어쨌든 에러가 나는 것은 이 핸들러 처리를 통해 잡을 수 가 있다 (어떻게?)
  //공통된 에러를 통하여 에러를 잡는 방식은 이전과 같은 방법으로 하면 될 것 같다.

  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(value = Exception.class)
  public Mono<String> errorHandler(ServerWebExchange exchange, Exception e) {
    return Mono.just("에러!");
  }

}
