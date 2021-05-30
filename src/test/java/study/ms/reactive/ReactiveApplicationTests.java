package study.ms.reactive;


import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;
import study.ms.reactive.service.SampleService;

@SpringBootTest
class ReactiveApplicationTests {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Test
  void contextLoads() {
  }

  @Autowired
  SampleService sampleService;

  //모노 , 플럭스 변환
  @Test
  public void convertMonoAndFlux() {
    Flux.just(1L, 2L).collectList(); //모노 리스트로 변환
    Mono.just(1L).flux(); //플럭스로 변환
    Flux<Long> fluxFrom = Flux.from(Flux.just(1L));
    Flux<Long> monoFrom = Flux.from(Mono.just(1L));
  }

  //defer와 just의 차이
  //defer는 레이지 로딩과 비슷하여 실행 시점에 실제적인 값을 받
  //just는  이거 로딩과 비슷하여 값을 설정하는 시점에(just 시점) 값을 설정함.
  @Test
  public void deferAndJustDifference() {
    Mono<Long> monoDefer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));
    System.out.println("start : " + System.currentTimeMillis());
    Mono<Long> monoJust = Mono.just(System.currentTimeMillis());
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    monoJust.subscribe(integer1 -> System.out.println("mono : " + integer1));
    monoDefer.subscribe(integer1 -> System.out.println("defer : " + integer1));
  }

  //Mono와 Flux 전환
  @Test
  public void etc() {
    Flux.just("1", "2")
        .collectList() //List가 담긴 모노로 전환
        .flux();  // 다시 리스트를 모노로 전환

    Mono.from(Flux.just("1", '2')); //플럭스 값을 모노로
    Flux.from(Mono.just("1"));     //모노값을 just로

    Flux<Integer> stream4 = Flux.range(2010, 9); //2010년도부터 시작하는 연도 만들기
  }


  @Test
  public void useDefer() throws InterruptedException {
    //블록킹을 걸어 바로 값을 구할 때
    //flux에 경우에는 toIterable 이나 toStream 등도 있다
    //blockfirst는 첫번째 값을 처리할 때까지  blocklast는 마지막값을 처리할 때 발생함
    sampleService.useDefer().block();
    //블로킹을 걸지 않고 작동만 시킬 때(이 경우 테스트 코드가 먼저 끝나 작동이 안될수도 있어서
    //테스트 코드를 짤 때는 아래 while 처럼 조건을 걸어두었다
    Disposable disposable = sampleService.useDefer().subscribe();

    while (!disposable.isDisposed()) {
      Thread.sleep(1000);
    }
  }

  //concat 두개의 플럭스를 연결함
  //buffer 각 작업을 buffer 수만큼 나뉘어 계산해서 배열로 리턴함
  @Test
  public void useConcatAndBuffer() {
    Flux.concat(Flux.range(1, 3), Flux.range(4, 2))
        .subscribe(System.out::println);

    Flux.range(1, 12).buffer(4).subscribe(System.out::println);
  }

  //then은 상위 작업이 완료되면, 완료된채로 리턴없이 끝낸다.
  //그 외 값을 넣는 then은 그 값으로 결과를 치환한다.
  @Test
  public void thenManyAndEmpty() {
    Flux.range(1, 12).thenMany(Flux.range(1, 5)).then();
  }


  //데이터와 시그널을 변환할 때 사용하려면 아래와 같이
  @Test
  public void useMaterializeAndDematerialize() {
    Flux.range(1, 5)
        .doOnNext(System.out::println)
        .materialize()
        .doOnNext(System.out::println)
        .blockLast();
  }

  //시퀀스를 자체적으로 만들려면 아래와 같이
  //아래 예제는 피보나치 수열 시퀀스임

  @Test
  public void useGenerate() {
    Flux.generate(() -> Tuples.of(0L, 1L),
        (state, sink) -> {
          sink.next("두번째 값 " + state.getT2());   //두번재 값을 onnext에 캡쳐할 수 있도록 신호를 보낸다.
          long newValue = state.getT1() + state.getT2();
          return Tuples.of(state.getT2(), newValue);
        })
        .take(10)
        .doOnNext(o -> {
          System.out.println(o);
        })
        .blockLast();
  }

  @Test
  public void 실패일_수_있는_작업_처리Sample() throws InterruptedException {
    Disposable disposable = sampleService.실패일_수_있는_작업_처리()
        .subscribe(
            b -> System.out.println("onnext " + b),  //성공시 처리
            e -> System.out.println("onerror" + e),  //실패시 처리 로그 작업>
            () -> System.out.println("onComplete")   //작업이 완료시 처리
        );

    while (!disposable.isDisposed()) {
      Thread.sleep(1000);
    }
  }

  @Test
  public void connectableFluxTest() throws InterruptedException {
    ConnectableFlux<Integer> connectableFlux = sampleService.connectableFluxSample();
    connectableFlux.connect();
  }

  @Test
  public void cashTest() throws InterruptedException {
    Flux<Integer> integerFlux = sampleService.cashSample();
    integerFlux.subscribe(e -> System.out.println("onnext 1번 " + e));
    integerFlux.subscribe(e -> System.out.println("onnext 2번 " + e));

    Thread.sleep(1200);

    integerFlux.subscribe(e -> System.out.println("onnext 3번 " + e));
  }


  @Test
  public void shareTest() throws InterruptedException {
    Flux<Integer> integerFlux = sampleService.shareSample();
    integerFlux.subscribe(e -> System.out.println("onnext 1번 " + e));
    Thread.sleep(2000);

    Disposable disposable = integerFlux.subscribe(e -> System.out.println("onnext 2번 " + e));
    while (!disposable.isDisposed()) {
      Thread.sleep(1200);
    }
  }

  @Test
  public void transFormTest() throws InterruptedException {
    Flux<String> stringFlux = sampleService.TransFormSample();
    stringFlux.subscribe();
    stringFlux.subscribe();
  }


  //그러면 지금까지 구독자 없이 subscribe()가 실행된 것은
  //내부에서 구독자를 알아서 만들어주기 때문
  //아래와 같이, 구독자를 직접 넣어 줄수도 있다.
  //여기서 onNext 상황일 때,
  //subscription의 request를 통해 직접 요청해야 하는데
  //구독자가 자기가 직접 구독 여부 시점을 결정하게 해서
  //자기가 구독할 상황이 될 상황을 판단하여 구독을 시작하게 하기 위함이다.
  //아래 글을 볼 것
  //링크 글 참고 : https://engineering.linecorp.com/ko/blog/reactive-streams-with-armeria-1/
  //그런데...(아래 테스트 케이스 참조)
  @Test
  public void subscriber() {

    Flux<Integer> stringFlux = Flux.just(1, 2, 3, 4, 5);
    stringFlux.subscribe(new Subscriber<Integer>() {

      org.reactivestreams.Subscription subscription;

      @Override
      public void onSubscribe(org.reactivestreams.Subscription subscription) {
        System.out.println("구독 시작");
        this.subscription = subscription;
        subscription.request(1);
      }

      @Override
      public void onNext(Integer item) {
        System.out.println("다음 아이템 : " + item);
        this.subscription.request(1);
      }

      ;

      @Override
      public void onError(Throwable throwable) {
        System.out.println("에러 : " + throwable.toString());
      }

      @Override
      public void onComplete() {
        System.out.println("완료!!! : ");
      }
    });

  }

  //위에처럼 구독자가 모두 커스터마이징을 하는 것은 좋지 않다고 한다.
  //리액티브에서 기본적으 제공하는 구독자는 안정성과 기능을 향상시킨 기능들을 제공하고 있기 때문이라고 한다.
  //그래서 위에 방법보다는 아래의 방법으로 하는 것을 권장한다고 한다.
  @Test
  public void baseSubscriber() {

    Flux<Integer> stringFlux = Flux.just(1, 2, 3, 4, 5);
    stringFlux.subscribe(new BaseSubscriber<Integer>() {

      @Override
      public void hookOnSubscribe(Subscription subscription) {
        request(1);
      }

      @Override
      public void hookOnNext(Integer value) {
        logger.debug("다음: {}", value);
        request(1);
      }
    });
  }


  //스케쥴러로 쓰레드 처리하기
  //publish on 한 시점부터
  //다른 쓰레드에게 이 일을 맡길 수가 있다.
  //publishOn을 함으로써
  //앞 부분은 워커가 작업을 하고
  //뒷 부분은 새로 만든 쓰레드가 작업을 한다
  //다음과 같은 방법으로 블록킹 작업만을 처리하는
  //쓰레드를 별로도 할당하여 작업을 주는 것 가능할 듯 하다.
  //(예를 들어 블럭킹 디비는 커넥션 풀에 제한이 있으니까
  // 커넥션 풀만큼의 쓰레드를 할당하여 작업하게 한다던지..)
  //그런데 publish on은 하나의 쓰레드에서
  //다른 쓰레드로 데이터를 넘겨주기 위해
  //내부적으로 큐를 쓴다고 한다. (큐를 쓰는 만큼 자원을 사용할 수 있다)
  @Test
  public void schedulerPublishOnTest() {

    logger.debug("test {}", 1L);

    Scheduler scheduler = Schedulers.boundedElastic();

    Flux.range(0, 100)
        .map(String::valueOf)
        .filter(s -> s.length() > 1)
        .map(o -> {
          logger.debug("a값" + o);
          return o + "a";
        })
        .publishOn(scheduler)
        .map(o -> {
          logger.debug("b값" + o);
          return o + "b";
        })
        .subscribe();
  }

  //subscribeon은 이 webflux를 사용할 스케쥴러를 지정한다.
  //지정하면 이 웹플러스를 처리할 때는 설정된 스케쥴러가 제공하는 쓰레드를 이용한다
  //적용 시점에 쓰레드가 바뀌는 publishon에 비하여,
  //subscribeOn은 upstream에 관여한다가 다운스트림으로 내려갈 때(publish를 만나기전까지) 작동한다
  //그래서 아래의 작업을 시키고 로그를 테스트해보면 퍼블리쉬on을 만나기 전까지
  //subscribe thread1 이 실행되어 있다.
  //(아래 subscribeOn를 지우고) 새로운 subscribeOn 을 publishOn보다 아래에 넣어도 마찬가지이다.
  //upstream 한다는 것은 webflux에 구독이 들어갈때, 만아래부터 요청이 들어가서 최대 요청이까지 올라갔다가. (upstream)
  //요청 결과를 아래로 downstream 하기 때문
  //upstream 할때의 변경한 쓰레드는 publishon이 만나기 전까지
  //지속된다.

  //책 203P
  //publishon은 작업을 하기 위해 큐를 만드는 과정에서 추가적인 연산이 들어가는데
  //subscribeon은 추가 큐를 만들지 않는다.
  //그래서 전체 작업을 네티 워커에게 맡기고 싶지 않다면,
  //subscribeon이 성능상 더 이점이 있을 것이다.
  @Test
  public void schedulerSubscribeOn() {
    //Scheduler elasticScheduler = Schedulers.boundedElastic(); ///쓰레드 범위가 가용으로 결정되는 스케쥴러라는 것 같다

    Scheduler elasticScheduler1 = Schedulers.newParallel("subscribe thread1");
    Scheduler elasticScheduler2 = Schedulers.newParallel("subscribe thread2");
    Scheduler newThreade = Schedulers
        .newParallel("publish thread");//현재의 쓰레드를 사용하고자할 때 사용(Test 코드에서는 Test Worker 쓰레드)

    logger.debug("여기 쓰레드는 어디냐?");

    Mono<String> mono = Mono.fromCallable(() -> {
      String a = "a";
      logger.debug("subscribeOn 스케쥴러 스레드 1" + a);
      return a;
    }).map((o) -> {
      logger.debug("subscribeOn 스케쥴러 스레드 2" + o);
      return o;
    })
        .subscribeOn(elasticScheduler1)
        .subscribeOn(elasticScheduler2); //

    Mono.defer(() -> mono)
        .map((o) -> {
          logger.debug("싱글 스케쥴러 스레드 " + o);
          return o;
        })
        .publishOn(newThreade)
        .map((o) -> {
          logger.debug("퍼블리시가 바뀐 " + o);
          return o;
        })
        .subscribe();
  }

  //context를 통해, 다른 영역에서 하나의 모노나 플럭스에서 설정한 값들을
  //가져오거나 처리할 수 있게 한다.
  //contextual -> 값을 가져와서 처리
  //contextwrite -> 값을 셋팅함
  //기존에는 subscriberconext를 이용하여, 값을
  //하나의 영역 내에서 put, get을 할 수 있었는데
  //저 기능은 deprecated 되었다.
  //지금은 각각 다른 영역에서 값을 put 하거나 get 하도록 해둔 거 같다 (왜?)
  //TODO  subscriberContext()는
  //put을 쓸때마다, context 객체를 새로 만들었다고 한다
  //(멀티 쓰레딩에서 위험한 공유를 막고자)
  //그런데 deferContextual 와  contextWrite도 마찬가지일까?
  @Test
  public void contextWriteText() {
    //deprecated된 기능
    //Mono.subscriberContext();

    //transformDeferredContextual

    String key = "message";
    Mono<String> r = Mono.just("Hello")
        .flatMap(s -> Mono.deferContextual(ctx -> {
          return Mono.just(s + " " + ctx.get(key));
        }))
        .contextWrite(ctx -> ctx.put(key, "World"));

    //테스트 결과로 사용하는 것
    StepVerifier.create(r)
        .expectNext("Hello World")
        .verifyComplete();
  }


  @Test
  public void DataBufferUtilsTest() {

    //파일이 없어, 작동하지는 않음. 이런식으로 파일을 읽는 것도
    //flux 형태로 바꿀 수 있음
    Flux<DataBuffer> reactiveHamlet = DataBufferUtils.read(
        new DefaultResourceLoader().getResource("halmet.txt"),
        new DefaultDataBufferFactory(),
        1024
    );
  }

  @Test
  public void deferContextualTest() {
    String key = "message";
    Mono<String> r = Mono.just("Hello").flatMap(s -> Mono.deferContextual(contextView -> {
      return Mono.just(s + " " + contextView.get(key));
    })).contextWrite(context -> {
      return context.put(key, "World");
    });
    StepVerifier.create(r).expectNext("Hello World").verifyComplete();
  }


  @Test
  public void testFluxFrom() throws Exception {
    Flux flux = Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    //flux_from은 이미 만들어진 flux에 정보로,
    //새로운 flux 로직을 새로 생성한다.
    //아래와 같이 두개의 flux from으로 만들면
    //각각 1,2,3,4,5,6,7,8,9,10을 호출하게 된다.
    Flux.from(flux)
        .doOnNext(o -> System.out.println("첫번째 플럭스 " + o)).subscribe();

    Flux.from(flux)
        .doOnNext(o -> System.out.println("두번째 플럭스 " + o)).subscribe();

    Disposable disposable = flux.subscribe();

    while (!disposable.isDisposed()) {
      Thread.sleep(1000);
    }
  }

  //TODO repeat도 위에서 적용하나, 아래에서 적용하나 차이가 없는 것 같다.( 확실치 않다)
  @Test
  public void fluxRepeatTest() {
    Flux.range(1, 3)
        .map(i -> {
          logger.info("map {} to {}", i, i + 2);
          return i + 2;
        })
        .flatMap(i -> {
          logger.info("flatMap {} to Flux.range({}, {})", i, 1, i);
          return Flux.range(1, i);
        }).repeat(10)
        .subscribe(i -> logger.info("next " + i));
  }


  //백프레셔레셔 조절.
  //delaySequence와, limitRate를 이용하여
  //하단으로 압력이 몰리는 것을 막기 위해 설정
  // 주의!!!!
  //해당 작업처럼 딜레이 시퀀스가 있을 경우에는
  //그 전에 있던 publishon subscribeon 설정이 안 먹힘
  //TODO 어디에서 관리하는지는 아직 모르는,cpu *2의 쓰레드가 관리함. 확인 필요
  @Test
  public void BackpressureTest() {
    Scheduler elasticScheduler1 = Schedulers.newParallel("subscribe thread1");


    Flux.range(0, 10)
        .publishOn(elasticScheduler1)  //안먹힘
        .delaySequence(Duration.ofMillis(100))
        .limitRate(2)
     //   .publishOn(elasticScheduler1)  //요건 먹힘
        .doOnNext(integer -> logger.debug("i = {}", integer))
        .collectList()
        .block();

  }


  //애러 처리 관련하여

  //flux에서 에러 처리할 때 중간에 플러스를 모노로 바꾸면, (collectList)
  //리스트에 결과를 받아야하기 때문에
  //하나가 에러 발생시 전체 결과를 받을 수 없다.
  //
  //플럭스로 할 경우에는
  //에러가 나는 지점이유부터 데이터를 받을수는 없지만.
  //onErrorContinue를 쓰면 에러 나는 부분만 제외하고 계속 작업을 진행할 수 있다.
  @Test
  public void errorTest() {
    Scheduler elasticScheduler1 = Schedulers.newParallel("subscribe thread1");


    Flux.range(0, 15)
        .flatMap((o)->{
          if(o == 8){
           throw new RuntimeException();
          }
          return Flux.just(o);
        })
        .onErrorContinue((o,c)->{System.out.println(o);})
        .doOnError((o)->System.out.println("에러!!!!!!!"))
        .doOnNext(o->System.out.println("여기다!!!" +o))
        .blockLast();

  }

}
