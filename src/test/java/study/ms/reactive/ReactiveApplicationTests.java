package study.ms.reactive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@SpringBootTest
class ReactiveApplicationTests {

	@Test
	void contextLoads() {
	}



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



}
