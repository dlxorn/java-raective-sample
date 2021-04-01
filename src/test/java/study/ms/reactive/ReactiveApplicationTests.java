package study.ms.reactive;



import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
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
    //테스트 코드를 짤 때는 아래 while 처럼 조건을 걸어두어야 한다.
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
    sampleService.connectableFluxSample().connect();
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

    Flux.range(0,100)
        .map(String::valueOf)
        .filter(s->s.length()>1)
        .map(o->{
          logger.debug("a값"  + o);
          return o+"a";
        })
        .publishOn(scheduler)
        .map(o->{
          logger.debug("b값"  + o);
        return  o+"b";
        })
        .subscribe();
  }


  //subscribeon은 아래처럼 callable 작업을 할 때(즉 외부에서 비동기 데이터를 가져올 때 라던지
  //작업할 워커를 새로운 워커에게 할당해주고 싶을 떄 사용한다.
  //그런데 이상한 것은 subscribeon으로 생성된 쓰레드는
  //그 뒤로 publish on을 해도 쓰레드 대상이 바뀌지 않는다.
  //TODO 좀 더 공부가 필요할 듯
  @Test
  public void schedulerSubscribeOn() {
    Scheduler elasticScheduler = Schedulers.boundedElastic(); ///쓰레드 범위가 가용으로 결정되는 스케쥴러라는 것 같다
    Scheduler immediateScheduler = Schedulers.immediate();       //현재의 쓰레드를 사용하고자할 때 사용(Test 코드에서는 Test Worker 쓰레드)

    logger.debug("여기 쓰레드는 어디냐?");

    Mono<String> mono =Mono.fromCallable(()-> {
      String a = "a";
      logger.debug("subscribeOn 스케쥴러 스레드 1" +a);
      return a;
    }).subscribeOn(elasticScheduler)
     .map((o)->{
       logger.debug("subscribeOn 스케쥴러 스레드 2"  +o);
       return o;
     });

    Mono.defer(()->mono)
        .map((o)->{
           logger.debug("싱글 스케쥴러 스레드 " + o);
          return o;
        })
        .publishOn(immediateScheduler)
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





}
