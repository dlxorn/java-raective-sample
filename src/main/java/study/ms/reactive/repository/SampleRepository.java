package study.ms.reactive.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import study.ms.reactive.collection.SampleCollection;

public interface SampleRepository extends ReactiveMongoRepository<SampleCollection, Long> {

}
