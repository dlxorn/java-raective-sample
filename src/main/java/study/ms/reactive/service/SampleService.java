package study.ms.reactive.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import study.ms.reactive.collection.SampleCollection;
import study.ms.reactive.collection.SampleWebClientCollection;
import study.ms.reactive.dto.SampleDTO;
import study.ms.reactive.dto.SampleWebclientDTO;
import study.ms.reactive.repository.SampleRepository;
import study.ms.reactive.repository.SampleWebClientRepository;

@Service
public class SampleService {

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


  public Mono<SampleWebClientCollection> postDataByWebClient(String id) {
    return webClient.get().uri("/todos/{id}", id)
        .retrieve().bodyToMono(SampleWebclientDTO.class)
        .log()
        //Exception handler에서 오류가 터지지 않도록 하기 위해
        //on ErrorResume를 쓰면, 에러난 값을 바꿀 순 있다.
        //그 결과가 아래처럼 쭉 내려간다.
     //   .onErrorResume((o)-> Mono.just(new SampleWebclientDTO()))
        .doOnNext((o) ->   System.out.println("next!"))
        .doOnSuccess((o) -> System.out.println("success!!"))
        .flatMap(o -> {
              System.out.println("진행 여부 확인");
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
  public Mono<SampleWebClientCollection> useDefer(){
    return Mono.defer(()->webClient.get().uri("/todos/{id}", 1)
        .retrieve().bodyToMono(SampleWebclientDTO.class))
        .flatMap(o -> {
      System.out.println("진행 여부 확인");
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
   return  Mono.just("hahaha")
        .flatMap(o->{
          throw new RuntimeException("강제 에러 처리");
        })
       .doOnError(o->System.out.println("do on error : " + o.toString()))
       .flatMap(o->{
          return Mono.just("haha");
       });
  }

  public Mono<List<String>> getFluxSample() {
    Mono<List<String>> listMono = Flux.just("a", "b", "c", "d", "e")
        .doOnNext(o->System.out.println("do on next 실행 시점 " +o))
        .doOnComplete(()->System.out.println("완료되었다 " ))
        .collectList();

    System.out.println("collect-list를 사용해서 mono를 변환해도 나중 시점에 실행된다");
    return listMono;
  }


  public Flux<String> 실패일_수_있는_작업_처리 (){
//    Flux.just("user-1")
//        .flatMap(user ->
//            caseService.recommendedBooks(user)
//            .retryWhen(r)

        return null;
  }









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


}
