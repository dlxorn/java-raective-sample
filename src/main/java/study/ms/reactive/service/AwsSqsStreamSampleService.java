package study.ms.reactive.service;


import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

//외부 요청에 의하여 큐를 쌓고,
//그 큐를 받아오고
//받아온 큐를 스트림이 가능한, webflux에 저장하는 구조
@Service
public class AwsSqsStreamSampleService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());


  @Autowired
  private SqsAsyncClient sqsAsyncClient;
  private String QueueName = "AwsSqsStream";
  final Sinks.Many sink;

  public AwsSqsStreamSampleService() {
    this.sink = Sinks.many().multicast().onBackpressureBuffer();
  }

  @PostConstruct
  public void postConstruct() {
    //TODO 리피트는 반복 작업을 할 때마다 쓰레드가 안바뀌고 인터벌은 관리하는 쓰레드가 바뀌는 것 같다. (확인 필요)
    //reqeustSqsRepeat();
    reqeustSqsInterval();
  }


  public Mono<String> asyncSendSampleMessageReturnMono(
      String message) {

    GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
        .queueName(QueueName)
        .build();

    return Mono.fromFuture(sqsAsyncClient.getQueueUrl(getQueueRequest)).flatMap(
        (o) -> {
          SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
              .queueUrl(o.queueUrl())
              .messageBody(message)
              .delaySeconds(5)
              .build();

          return Mono.fromFuture(sqsAsyncClient.sendMessage(sendMsgRequest));
        }
    ).then(Mono.just("SEND"));
  }

  public Flux<String> stream() {
    return sink.asFlux().map(e -> ServerSentEvent.builder(e).build());
  }

  public void reqeustSqsRepeat() {
    Mono.fromFuture(
        sqsAsyncClient
            .getQueueUrl(GetQueueUrlRequest.builder().queueName(QueueName).build())
    ).repeatWhen(completed -> completed.delaySequence(Duration.ofSeconds(1L)))
        .timeout(Duration.ofSeconds(10))
        .log().flatMap(o -> {
      return Mono.deferContextual(contextView -> {
        Map<String, Object> map = contextView.get("ContextMap");
        map.put("queueUrl", o.queueUrl());
        return Mono.just(o);
      });
    }).
        flatMap((o) -> {
          CompletableFuture<ReceiveMessageResponse> receiveMessageResponse = sqsAsyncClient
              .receiveMessage(
                  ReceiveMessageRequest.builder()
                      .maxNumberOfMessages(5)
                      .queueUrl(o.queueUrl())
                      .waitTimeSeconds(10)
                      .visibilityTimeout(30)
                      .build()
              );
          return Mono.fromFuture(receiveMessageResponse);
        })
        .doOnNext(o -> o.messages().stream().forEach((c) -> sink.tryEmitNext(c.body())))
        .flatMap(o -> Mono.deferContextual(contextView -> {
          Map<Object, Object> map = contextView.get("ContextMap");
          String queueUrl = (String) map.get("queueUrl");

          List<CompletableFuture> list = new ArrayList<>();
          o.messages().stream().forEach((c) -> {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(c.receiptHandle())
                .build();
            list.add(sqsAsyncClient
                .deleteMessage(deleteMessageRequest));
          });
          return Mono.just(list);
        })).flatMap((o) -> Mono.just(o).flatMapMany(Flux::fromIterable))
        //contextWrite 사용할 때에 주의점
        //해당 값은 webflux 요청 끼리 모두 공유한다.
        //상단해서 특정한 요소만 처리하기 위한 결과만을 처리하기 위해
        //컨텍스트 라이트를 쓰기는 조금 어려울 것 같다.
        //위 로직에서 사용시 문제가 없는 것은 queryUrl값이 같은 값이기 때문
        .contextWrite(ctx -> ctx.put("ContextMap", new HashMap<String, Object>()))
        .subscribeOn(Schedulers.single()).
        subscribe();
  }


  //위에랑 처리 로직은 같은데
  //여기는 인터벌을 쓰고,
  //컨텍스트맵을 사용안함.
  public void reqeustSqsInterval() {

    Flux.interval(Duration.ofSeconds(1L))
        .flatMap((o) -> Mono.fromFuture(
            sqsAsyncClient
                .getQueueUrl(GetQueueUrlRequest.builder().queueName(QueueName).build())
        )).timeout(Duration.ofSeconds(10))
        .log()          //로그도 삭제할까?
        .doOnNext(o -> {
          String queueUrl = o.queueUrl();
          CompletableFuture<ReceiveMessageResponse> receiveMessageResponse = sqsAsyncClient
              .receiveMessage(
                  ReceiveMessageRequest.builder()
                      .maxNumberOfMessages(5)
                      .queueUrl(o.queueUrl())
                      .waitTimeSeconds(10)
                      .visibilityTimeout(30)
                      .build()
              );
          Mono.fromFuture(receiveMessageResponse)
              .doOnNext((d) -> d.messages().stream().forEach((c) -> {
                sink.tryEmitNext(c.body());
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(c.receiptHandle())
                    .build();
                Mono.fromFuture(sqsAsyncClient
                    .deleteMessage(deleteMessageRequest))
                    .doOnSuccess((t)->logger.debug("depth 3 로그 : {}",t))
                    .subscribe();
              }))
              .doOnSuccess((t)->logger.debug("depth 2 로그 : {}",t))
              .subscribe();
        })
        .doOnNext((t)->logger.debug("depth 1 로 : {}",t))
        .subscribe();
  }


}
