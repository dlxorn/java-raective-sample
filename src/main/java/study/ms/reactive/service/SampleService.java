package study.ms.reactive.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import study.ms.reactive.collection.SampleCollection;
import study.ms.reactive.dto.SampleDTO;
import study.ms.reactive.repository.SampleRepository;

@Service
public class SampleService {

  @Autowired
  private SampleRepository sampleRepository;


  public Mono<SampleCollection> getSample(){
    return  sampleRepository.findById(1L);
  }


  //MoNo로 전달한다는 것은 최종적으로 해당 작업들이 netty에서 실행됨을 의미하낟
  //그래서 최종적으로 전달할 Mono나 Flux에 여러 작업들을 엮어 두지 않으면, 실행 안된다
  public Mono<SampleCollection>  postSampleWithMonoDTO(SampleDTO sampleDTOMono) {
    SampleCollection sampleCollection = new SampleCollection();
    sampleCollection.setFirstname(sampleDTOMono.getFirstname());
    sampleCollection.setLastname(sampleDTOMono.getLastname());

   return sampleRepository.save(sampleCollection);
  }
}
