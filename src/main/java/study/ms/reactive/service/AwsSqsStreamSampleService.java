package study.ms.reactive.service;


import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
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

  @Autowired
  private SqsAsyncClient sqsAsyncClient;
  private String QueueName = "AwsSqsStream";
  final Sinks.Many sink;

  public AwsSqsStreamSampleService() {
    this.sink = Sinks.many().multicast().onBackpressureBuffer();
  }

  @PostConstruct
  public void postConstruct() {
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

          List<CompletableFuture> list=  new ArrayList<>();
          o.messages().stream().forEach((c) -> {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(c.receiptHandle())
                .build();
            list.add(sqsAsyncClient
                .deleteMessage(deleteMessageRequest));
          });
          return Mono.just(list);
        })).flatMap((o)-> Mono.just(o).flatMapMany(Flux::fromIterable) )
        .contextWrite(ctx -> ctx.put("ContextMap", new HashMap<String, Object>()))
        .subscribeOn(Schedulers.single()).
        subscribe();
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


}
