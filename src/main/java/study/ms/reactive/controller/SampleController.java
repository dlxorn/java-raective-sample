package study.ms.reactive.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import study.ms.reactive.collection.SampleCollection;
import study.ms.reactive.collection.SampleWebClientCollection;
import study.ms.reactive.dto.SampleDTO;
import study.ms.reactive.dto.SampleWebclientDTO;
import study.ms.reactive.service.SampleService;

@RestController
@RequestMapping("/sample")
public class SampleController {

  @Autowired
  private SampleService sampleService;


  //이렇게 Mono를 쓰는 것만으로도 자연스럽게 webflux가 처리가 되나?
  //사용하는 객체 중에 특별히 어떤 설정들을 하지 않아도?

  //Mono와 Flux는 실제로 구독이 이루어져야 데이터를 만들어내므로
  //실제 스프링에서 자동으로  구독을 실행하여 해당 작업들을 진행시킨다고 생각하면 될 것 같다
  //아래처럼 Serverexchange를 사용해서, 가져올 수도 있고, 우측에   request를 통해서 가져올 수도 있다.

  //다음에는 repository에서 어떻게 데이터를 가져와서 처리 하는지 확인이 필요할 듯 하다.

  @GetMapping
  public Mono<SampleCollection> getSample(ServerWebExchange exchange, ServerHttpRequest request) {
    var dsada =exchange.getRequest().getQueryParams();
    System.out.println(dsada.get("id"));
    System.out.println(request.getQueryParams().get("id"));
    return sampleService.getSample();
  }

  //DTO를 선언해서 가져올 수도 있
  @GetMapping("/dto")
  public Mono<String> getSampleWithDTO(SampleDTO sampleDTO) {
    System.out.println(sampleDTO.getId());
    return Mono.just("end");
  }

  //DTO를 Mono로 감싸서  가져올 수도 있음 그러나 성능 차이는 없는 것 같다.
  @GetMapping("/monoDTO")
  public Mono<String> getSampleWithMonoDTO(Mono<SampleDTO> sampleDTOMono) {
    System.out.println(sampleDTOMono.block().getId());
    return Mono.just("end");
  }

  //JSON 형식으로 보낸 것도 리퀘스트 바디를 통해 잘 받는다.
  @PostMapping(value="/monoDTO",consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<SampleCollection> postSampleWithMonoDTO(@RequestBody  SampleDTO sampleDTOMono) {
    System.out.println(sampleDTOMono.getId());
    return sampleService.postSampleWithMonoDTO(sampleDTOMono);
  }

  //JSON 형식으로 보낸 것도 리퀘스트 바디를 통해 잘 받는다.
  @GetMapping(value="/string-return")
  public String getStringReturn() {
    //그냥 모노없이 스트링을 보내도 잘 처리가 되긴 함.
    //그런데 이렇게 결과값을 바로 보낼거면,
    //jva reactive를 사용하는 이유가 없을 듯
    return "end";
  }



  //JSON 형식으로 보낸 것도 리퀘스트 바디를 통해 잘 받는다.
  @GetMapping(value="/webclient-return/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<SampleWebclientDTO> getDataByWebClient(@PathVariable("id") String id) {
    return sampleService.getDataByWebClient(id);
  }


  @PostMapping(value="/webclient-return/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<SampleWebClientCollection> postDataByWebClient(@PathVariable("id") String id) {
    return sampleService.postDataByWebClient(id);
  }

  @GetMapping(value="/do-error")
  public Mono<String> doError() {
    return sampleService.doError();
  }

  //컨텍스트 테스트
  @GetMapping(value="/context")
  public Mono<String> useContext(){
   // return sampleService.useContext();
    return Mono.just("value");
  }

  //flux 시작 flux로 시작했는데, 리스트로 넘기려면 리스트가 담긴 모노로 바꾸어줘야 해서 이렇게 됐다.
  @GetMapping(value="/flux")
  public Mono<List<String>> getFluxSample() {
    return sampleService.getFluxSample();
  }


}
