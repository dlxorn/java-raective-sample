package study.ms.reactive.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import study.ms.reactive.collection.SampleCollection;
import study.ms.reactive.collection.SampleWebClientCollection;

public interface SampleWebClientRepository  extends
    ReactiveMongoRepository<SampleWebClientCollection, Long> {

}
