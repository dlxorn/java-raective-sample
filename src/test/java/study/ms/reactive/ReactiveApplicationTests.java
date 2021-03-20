package study.ms.reactive;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import study.ms.reactive.repository.SampleWebClientRepository;
import study.ms.reactive.service.SampleService;

@SpringBootTest
class ReactiveApplicationTests {

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
		System.out.println("start : "+ System.currentTimeMillis());
		Mono<Long> monoJust = Mono.just(System.currentTimeMillis());
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		monoJust.subscribe(integer1 -> System.out.println("mono : " +integer1));
		monoDefer.subscribe(integer1 -> System.out.println("defer : " +integer1));
	}

	//Mono와 Flux 전환
	@Test
	public void etc(){
		Flux.just("1","2")
				.collectList() //List가 담긴 모노로 전환
		    .flux();  // 다시 리스트를 모노로 전환

		Mono.from(Flux.just("1",'2')); //플럭스 값을 모노로
		Flux.from(Mono.just("1"));     //모노값을 just로

		Flux<Integer> stream4 =Flux.range(2010, 9) ; //2010년도부터 시작하는 연도 만들기
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
  public void useConcatAndBuffer(){
			Flux.concat(Flux.range(1,3), Flux.range(4,2))
					.subscribe(System.out::println);

			Flux.range(1,12).buffer(4).subscribe(System.out::println);
	}

	//then은 상위 작업이 완료되면, 완료된채로 리턴없이 끝낸다.
	//그 외 값을 넣는 then은 그 값으로 결과를 치환한다.
	@Test
	public void thenManyAndEmpty(){
		Flux.range(1,12).thenMany(Flux.range(1,5)).then();
	}


	//데이터와 시그널을 변환할 때 사용하려면 아래와 같이
	@Test
	public void useMaterializeAndDematerialize(){
		Flux.range(1,5)
				.doOnNext(System.out::println)
				.materialize()
				.doOnNext(System.out::println)
				.blockLast();
	}

	//시퀀스를 자체적으로 만들려면 아래와 같이
	//아래 예제는 피보나치 수열 시퀀스임

	@Test
	public void useGenerate(){
		Flux.generate(()-> Tuples.of(0L,1L),
				(state, sink) ->{
			         sink.next("두번째 값 "+ state.getT2());   //두번재 값을 onnext에 캡쳐할 수 있도록 신호를 보낸다.
			         long newValue = state.getT1() + state.getT2() ;
			         return  Tuples.of(state.getT2(),  newValue);
				})
				.take(10)
				.doOnNext(o->{System.out.println(o);})
				.blockLast();
	}

	@Test
	public void 실패일_수_있는_작업_처리Sample() throws InterruptedException{
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
	public void connectableFluxTest() throws InterruptedException{
		sampleService.connectableFluxSample().connect();
	}

	@Test
	public void cashTest() throws InterruptedException {
		Flux<Integer> integerFlux = sampleService.cashSample();
		integerFlux.subscribe(e->System.out.println("onnext 1번 " +e));
		integerFlux.subscribe(e->System.out.println("onnext 2번 " +e));

		Thread.sleep(1200);

		integerFlux.subscribe(e->System.out.println("onnext 3번 " +e));
	}


	@Test
	public void shareTest() throws InterruptedException {
		Flux<Integer> integerFlux = sampleService.shareSample();
		integerFlux.subscribe(e->System.out.println("onnext 1번 " +e));
		Thread.sleep(2000);

		Disposable disposable = integerFlux.subscribe(e -> System.out.println("onnext 2번 " + e));
			while (!disposable.isDisposed()) {
				Thread.sleep(1200);
			}
	}

	@Test
	public void transFormTest() throws InterruptedException {
		Flux<String> stringFlux =  sampleService.TransFormSample();
		stringFlux.subscribe();
		stringFlux.subscribe();
	}





}
