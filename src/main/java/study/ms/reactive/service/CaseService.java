package study.ms.reactive.service;


import java.time.Duration;
import java.util.Random;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


//여러 사항이 발생할 수 있는 다양한 케이스를 전달하는 서비스
@Service
public class CaseService {

  private final Random rnd = new Random();

  //에러를 일으킬수도 안일으킬수도 있는 랜덤 결과 전달
  public Flux<String> recommendedBooks(String userId){
    return Flux.defer(()->{
      if(rnd.nextInt(10) < 7){
        return Flux.<String>error(new RuntimeException("err")).delaySequence(Duration.ofMillis(100));
      }else{
        return  Flux.just("blue Mars" , "The expanse").delayElements(Duration.ofMillis(100));
      }
    }).doOnSubscribe(s->System.out.println(s));
  }

}
