package study.ms.reactive.controller;


import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import study.ms.reactive.dto.MessageDTO;

@RestController
@RequestMapping("/stream")
public class StreamController {

  //many는 flux로 값을 전달한다는 듯?
  final Sinks.Many sink;

  public StreamController() {
                       //TODO 메서드의 의미가 무엇인지 확인할 것
    this.sink =  Sinks.many().multicast().onBackpressureBuffer();
  }



  @GetMapping("/send")
  public boolean addValue(){
    //특이한 것은 제네릭을 쓰지 않아도, 아래 객체가 json 형태로 파싱되어 전달된다.
    //안에서 json으로 변경해주는 로직이 있을 듯
    sink.tryEmitNext(new MessageDTO());

    return true;
  }


  //WEBSOCKET 이용한 스트림 데이터 전달을 사용한다.
  //Sinks는 webflux에   push를 하게 해준다.
  //sink에 들어온 데이터들은 sink에 의한 만들어진  webflux에 지속적으로 푸시하게ㄴ 된다.
  //이 메세지를 TEXT_EVENT_STREAM_VALUE를 통하여 (웹소켓을 통하여) 지속적인 전달이
  //가능하게 된다.
  //참고)https://stackoverflow.com/questions/51370463/spring-webflux-flux-how-to-publish-dynamically
  //책 참고 292쪽(책에서는 ReplayProcessor가 존재하나 depreacted 되었음)
  @GetMapping(value="/receive" ,produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> stream(){
                                  //TODO ServerSentEvent event도 뭔지 확인 필요. 안써도 객체 결과는 나오나..확인이 필요하다
    return sink.asFlux().map(e -> ServerSentEvent.builder(e).build());
  }


}
