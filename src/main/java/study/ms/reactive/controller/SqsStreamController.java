package study.ms.reactive.controller;


import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import study.ms.reactive.service.AwsSqsStreamSampleService;

//외부 요청에 의하여 큐를 쌓고,
//그 큐를 받아오고
//받아온 큐를 스트림이 가능한, webflux에 저장하는 구조
@RestController
@RequestMapping("/sqs-stream")
public class SqsStreamController {

  @Autowired
  AwsSqsStreamSampleService awsSqsStreamSampleService;

  @GetMapping("/send")
  public Mono<String> addValue() {
    String massage = "request : " + LocalDateTime.now().toString();
    return awsSqsStreamSampleService.asyncSendSampleMessageReturnMono(massage);
  }

  @GetMapping(value = "/receive-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<String> streamValue() {
    return awsSqsStreamSampleService.stream();
  }


}
