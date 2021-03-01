package study.ms.reactive.service;

import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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

  //테스트 용도로 쓰는 url
  //https://jsonplaceholder.typicode.com/todos/2
  public SampleService(SampleRepository sampleRepository, WebClient.Builder webClientBuilder,
      SampleWebClientRepository sampleWebClientRepository) {
    this.sampleRepository = sampleRepository;
    this.webClient = webClientBuilder.baseUrl("https://jsonplaceholder.typicode.com").build();
    this.sampleWebClientRepository = sampleWebClientRepository;
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
    // TODO flatmap과 map 의 차이 확인 필요!
    return   webClient.get().uri("/todos/{id}", id)
        .retrieve().bodyToMono(SampleWebclientDTO.class)
        .flatMap(o -> {
              SampleWebClientCollection sampleWebClientCollection = new SampleWebClientCollection();
              sampleWebClientCollection.setCompleted(o.getCompleted());
              sampleWebClientCollection.setId(o.getId());
              sampleWebClientCollection.setTitle(o.getTitle());
              sampleWebClientCollection.setUserId(o.getUserId());
              return sampleWebClientRepository.save(sampleWebClientCollection);
            }
        );
  }



}
