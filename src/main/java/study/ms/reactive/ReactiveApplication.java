package study.ms.reactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveApplication.class, args);
	}

}

//TODO
//Log()를 사용하지 말 것  (log는 싱크로나이즈로 작동한다)
//map()의 사용을 자제할 것 map은 flatmap과 다르게 싱크로나이즈로 작동한다.
//또 너무 많은 map의 조합은 연산마다 객체를 생성한다.
//Mono객체를 스케쥴러에서 등록해서 스케쥴러가 처리하게끔 할 수 있나?