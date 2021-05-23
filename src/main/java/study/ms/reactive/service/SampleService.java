package study.ms.reactive.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties.Netty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;
import study.ms.reactive.collection.SampleCollection;
import study.ms.reactive.collection.SampleWebClientCollection;
import study.ms.reactive.dto.SampleDTO;
import study.ms.reactive.dto.SampleWebclientDTO;
import study.ms.reactive.repository.SampleRepository;
import study.ms.reactive.repository.SampleWebClientRepository;

@Service
public class SampleService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());


  private final SampleRepository sampleRepository;
  private final SampleWebClientRepository sampleWebClientRepository;
  private final WebClient webClient;
  private final CaseService caseService;

  //테스트 용도로 쓰는 url
  //https://jsonplaceholder.typicode.com/todos/2
  public SampleService(SampleRepository sampleRepository, WebClient.Builder webClientBuilder,
      SampleWebClientRepository sampleWebClientRepository, CaseService caseService) {
    this.sampleRepository = sampleRepository;
    this.webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com").build();
    this.sampleWebClientRepository = sampleWebClientRepository;
    this.caseService = caseService;
  }


  public Mono<SampleCollection> getSample() {
    return sampleRepository.findById(1L);
  }


  //MoNo로 전달한다는 것은 최종적으로 해당 작업들이 netty에서 실행됨을 의미하낟
  //그래서 최종적으로 전달할 Mono나 Flux에 여러 작업들을 엮어 두지 않으면, 실행 안된다
  public Mono<SampleCollection> postSampleWithMonoDTO(SampleDTO sampleDTOMono) {
    SampleCollection sampleCollection = new SampleCollection();
    sampleCollection.setFirstname(sampleDTOMono.getFirstname());
    sampleCollection.setLastname(sampleDTOMono.getLastname());

    return sampleRepository.save(sampleCollection);
  }


  public Mono<SampleWebclientDTO> getDataByWebClient(String id) {
    return webClient.get().uri("/todos/{id}", id).retrieve().bodyToMono(SampleWebclientDTO.class)
        .log();
  }


  //webclient 요청에 대하여 repteat를 넣어서 지속적으로 요청을 하는 형태로 사용할 수 있다.
  //repeatwhen을 사용하여, repeat시에 조건을 여러가지로 넣는 것이 가능하다.
  //딜레이를 넣어서 처리하는 것도 가능하고, 혹은,타이머를 정해서 작업을 진행시키는 것도 가능할 듯.
  public Flux<SampleWebclientDTO> getStreamDataByWebClient(String id) {
    return webClient.get().uri("/todos/{id}", id).retrieve().bodyToMono(SampleWebclientDTO.class)
        .repeatWhen(completed -> completed.delaySequence(Duration.ofSeconds(1L)))
        .timeout(Duration.ofSeconds(10))
        .log();
  }


  public Mono<SampleWebClientCollection> postDataByWebClient(String id) {
    return webClient.get().uri("/todos/{id}", id)
        .retrieve().bodyToMono(SampleWebclientDTO.class)
        .log()
        //Exception handler에서 오류가 터지지 않도록 하기 위해
        //on ErrorResume를 쓰면, 에러난 값을 바꿀 순 있다.
        //그 결과가 아래처럼 쭉 내려간다.
        //   .onErrorResume((o)-> Mono.just(new SampleWebclientDTO()))
        .doOnNext((o) -> logger.debug("next!"))
        .doOnSuccess((o) -> logger.debug("success!!"))
        .flatMap(o -> {
              logger.debug("진행 여부 확인");
              SampleWebClientCollection sampleWebClientCollection = new SampleWebClientCollection();
              sampleWebClientCollection.setCompleted(o.getCompleted());
              sampleWebClientCollection.setId(o.getId());
              sampleWebClientCollection.setTitle(o.getTitle());
              sampleWebClientCollection.setUserId(o.getUserId());
              return sampleWebClientRepository.save(sampleWebClientCollection);
            }
        );
  }

  //TODO 확인 필요
  //defer로 위처럼 webcleint로 이어지는 것이 아니라 를 엮어서 쓰는 것도 가능하다
  //위에랑 같은 방식으로 작동할 거라고 생각되지만, 확인이 필요할듯 하다.
  public Mono<SampleWebClientCollection> useDefer() {
    return Mono.defer(() -> webClient.get().uri("/todos/{id}", 1)
        .retrieve().bodyToMono(SampleWebclientDTO.class))
        .flatMap(o -> {
          logger.debug("진행 여부 확인");
          SampleWebClientCollection sampleWebClientCollection = new SampleWebClientCollection();
          sampleWebClientCollection.setCompleted(o.getCompleted());
          sampleWebClientCollection.setId(o.getId());
          sampleWebClientCollection.setTitle(o.getTitle());
          sampleWebClientCollection.setUserId(o.getUserId());
          return sampleWebClientRepository.save(sampleWebClientCollection);
        });
  }


  //내부에서 발생한 에러를 exception-handler에서 잡을 수 있다.
  public Mono<String> doError() {
    return Mono.just("hahaha")
        .flatMap(o -> {
          throw new RuntimeException("강제 에러 처리");
        })
        .doOnError(o -> logger.debug("do on error : " + o.toString()))
        .flatMap(o -> {
          return Mono.just("haha");
        });
  }

  public Mono<List<String>> getFluxSample() {
    Mono<List<String>> listMono = Flux.just("a", "b", "c", "d", "e")
        .doOnNext(o -> logger.debug("do on next 실행 시점 " + o))
        .doOnComplete(() -> logger.debug("완료되었다 "))
        .collectList();

    logger.debug("collect-list를 사용해서 mono를 변환해도 나중 시점에 실행된다");
    return listMono;
  }


  public Flux<String> 실패일_수_있는_작업_처리() {

    return Flux.just("user")
        .flatMap(user ->
            caseService.recommendedBooks(user)
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(10000)))
                //실패시 리트라이 횟수
                //실패하면 retry부터 다시 하므로, subscirbe 로그부터 다시 발생한다
                .timeout(Duration.ofSeconds(3))         //최대 기다려 줄 있는 시간 //처리가 안되면 에러로 발행된다.
                .onErrorResume(e -> Flux.just("The Martian")));
  }


  //ConnectableFlux를 connect 했을 때 그 시점부터, publish하고 subscriber가 데이터를 받을 수 있음.
  public ConnectableFlux<Integer> connectableFluxSample() {
    Flux<Integer> source = Flux.range(0, 3)
        .doOnNext(o -> logger.debug("본체" + o))
        .doOnSubscribe(
            o -> logger.debug("new subscription for the cold publisher ")); //TODO 이게 왜 cold지?

    ConnectableFlux<Integer> conn = source.publish();
    try {
      logger.debug("잠시 대기");
      Thread.sleep(1000);
    } catch (Exception e) {
      logger.debug("에러!!");
    }
    conn.subscribe(o -> logger.debug("subscriber 1 " + o));
    conn.subscribe(o -> logger.debug("subscriber 2 " + o));
    logger.debug("잠시 또 대기");
    return conn;
  }

  //ConnectableFlux 처럼 하나의 데이터를 받아서 지속적으로 구독받을 수 있는데
  //한가지 더 장점은 지속 시간을 두어 해당 데이터를 캐시하는 순간을
  //정해둘 수 있다는 것이다.캐시  대기 시간 이후에 발생하는 요청은
  //다시 데이터를 만들어두어 캐싱한다
  public Flux<Integer> cashSample() {
    Flux<Integer> source = Flux.range(0, 2)
        .doOnSubscribe(s -> logger.debug("integer value : {}", s));

    return source.cache(Duration.ofSeconds(1));
  }

  //첫번째 구독 이후부터 발생한 데이터를
  //다음 구독자들도 순차적으로 받게끔 할 때(이미 지나간 시퀀스는 무시)
  public Flux<Integer> shareSample() {
    Flux<Integer> source = Flux.range(0, 5)
        .delayElements(Duration.ofMillis(1000))   //delay를 가지고 생성
        .doOnSubscribe(s -> logger.debug("new subscription for the cold publisher"));
    return source.share();
  }

  //TransForm 사용 Flux를 flux로 리턴하는 함수를 중간에 끼어넣어 그 함수가 처리할 수 있게 해줌
  //TransForm을 사용할 때 주의할 것은, 실제로 이 함수는 구독 시마다
  //아래 함수가 실행되지 않는다는 점이다
  //그래서  logger.debug("여기 몇번 왔을까?"); 는 딱 한번 발생하게 된다
  //실제로는
  //  tringFlux.index()  //다음값은 tuple로 인덱스를 가져오는 값을 처리
  //      .doOnNext(o -> logger.debug("get1 " + o.getT1() + " " + "get2 " + o.getT2()))
  //      .map(Tuple2::getT2);
  //요것만 전달받고 처리하는 셈이 된다.
  //만약 구독시마다, 함수를 새로 실행하여 처리하고 싶다면??(가령 조건에 따라 flux의 상태값들을 다르게 처리할 필요가 있다던지)
  //compose 연산자를 사용하면 된다
  public Flux TransFormSample() {

    Function<Flux<String>, Flux<String>> logUserInfo = stringFlux -> {
      logger.debug("여기 몇번 왔을까?");
      return stringFlux.index()  //다음값은 tuple로 인덱스를 가져오는 값을 처리
          .doOnNext(o -> logger.debug("get1 " + o.getT1() + " " + "get2 " + o.getT2()))
          .map(Tuple2::getT2);
    };

    return Flux.range(1000, 3)
        .map(i -> "user - " + i)
        .transform(logUserInfo);
  }

  private final ExecutorService threadPool = Executors.newFixedThreadPool(1);
  private Scheduler scheduler = Schedulers.fromExecutor(threadPool);

  AtomicInteger atomicInteger = new AtomicInteger(0);


  //내부에서 subscribe()를 하면 어떻게 작동하는지를 확인하는 테스트
  //다행히도 내부에서 subscribe() 를 해도,
  //netty에서 제공되는 워커로 작업을 하는 것 같다....
  //만약에 아니더라도,  스케쥴러를 새로 생성해서 정해진 갯수의 쓰레드로
  //처리하게 하면 문제는 없을 것 같다.
  //다만 주의할 것이 있다.
  //아래의 delaySequence를 사용할 경우
  //delay sequence 아래의 작업들은, 새로운 영역의 쓰레드에서 작업이 진행되는 것 같다.
  //아마도 대기가 발생할때 쓰레드가 바뀌기 때문에 그렇게 해둔 것 같은데
  //netty가 관리하는 워커의 영역은 아닌 것 같다.
  //테스트한 바로는  이 쓰레드도 cpu *2이상의 갯수로 실행되지는 않는다.
  //무한 쓰레드 문제는 생기지 않을 것 같아 다행이긴 한데.
  //TODO 컨트롤할 수 있는지 확인 필요하다.
  //delaySequence 아래 publishOn을 넣으면 publishon으로
  //설정한 스케쥴러로 작동하기는 하지만, 바로 그 전작업까지는 다른 스레드가
  //그 작업을 하고 있었을 것이다.
  public Flux<String> runInnerSubscribe() {
    int i = atomicInteger.getAndIncrement();

    return Flux.range(0, 1000).flatMap((o) -> {
            logger.debug("전담하는 쓰레드 확인  1  ");
          Flux.just("1", "2", "3", "4")
              //   .delaySequence(Duration.ofMillis(2000))
              //    .limitRate(2)
                  .publishOn(scheduler)
              .map((c) -> {
                logger.debug("sdsdsds 요청 순번 : {} ,첫번째 :{}, 두번째 : {}", i, o, c);
                return c;
              })
              .doOnNext((c) -> logger.debug("요청 순번 : {} ,첫번째 :{}, 두번째 : {}", i, o, c))
              //     .subscribeOn(scheduler)
              .subscribe();

          webClient.get().uri("/todos/{id}", 1)
              .retrieve().bodyToMono(SampleWebclientDTO.class)
              .doOnSuccess((d) -> logger.debug("결과 : {}", d))
              .subscribe();

          logger.debug("전담하는 쓰레드 확인  2 ");
          return Flux.just("" + o);
        }
    );
  }


}

//  public Flux fluxMergeSequential() {
//    //두개의 스트림을 동시에 구독하지만,
//    //첫번째 스트림이 끝나야 두번째 스트림의 구독을 시작함
//    Flux.mergeSequential()
//    return null;
//  }

//elapsed
//이전 스트림과의 간격을 확인하고자할 때 시용한다
//elapsed()
//subscribe(e -> System.out.prinln(e.getT1(), egetT2());

//contextrite는 사용법의 확인이 필요하다
//  public Mono<String> useContext() {
//    Mono.just("A").contextWrite(context -> context.put("test-key",context))
//        .map(o-> "b");
//    Mono.deferContextual(contextView-> {
//      String
//    });
//
//    return
//  }

//주의
//map을 사용하면 새로운 Mono나 Flux 객체가 생성된다.
// Flux<String> flux = Flux.just("A")
// flux.map(i -> "foo" + i)
// flux.subscribe(System.out::println)  -> 이렇게 하면 map의 결과가 나오질 않는다.

//Processor 연산자란 것도 있는데
// 일단 이건 쓰는 것을 권장하지 않는다고 하니 넘어가자

//then과 concatwith의 차이.
//then은 특정 작업의 완료를 다른 갑으로 대체할 때 사용하고(그전 값이 뭐든 상관없이)
//concatwith는 특정 시퀀스 작업이 끝나면 그 다음에 시퀀스 작업을 진해야할 때 사용
//(그 전 작업이 끝난 후 concatwith 작업도 구독)
//startwith 는 시작할 때 같이 시작함

//reactor api 위치
//https://projectreactor.io/docs/core/release/api/
//기타
//https://projectreactor.io/docs/core/release/api/reactor/core/publisher/ReplayProcessor.html

//책 291쪽에 new parameterizedTypeReference<>() 가 뭔지 테스트해보고 확인할 것

