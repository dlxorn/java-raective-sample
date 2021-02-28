package study.ms.reactive.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

//자바 리액티브로 서비스 단은 어떻게 구현할까?
@Service
public class SampleService {

  public Mono<String> getSampleData(){
    return Mono.just("1");
  }



}
