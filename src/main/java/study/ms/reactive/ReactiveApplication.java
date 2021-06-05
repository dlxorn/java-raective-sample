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
//Log()를 사용하지 말 것  (log는 싱크로나이즈로 작동한다) - 작업완료
//map()의 사용을 자제할 것 map은 flatmap과 다르게 싱크로나이즈로 작동한다.
//map()을 최소화하고, map으로 io 작업이 일어나는 부분들은 flatmap 으로 변경 해야한다
//또 map의 조합은 연산마다 객체를 생성한다. 홧실치 않지만 아마 flatmap도 비슷할 것 같다. mapping 작업은 여러 함수 안쓰고 한번에 처리 하는 것을 목표로 하자.