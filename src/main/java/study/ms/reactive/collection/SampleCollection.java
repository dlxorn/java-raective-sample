package study.ms.reactive.collection;


import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "sample")
public class SampleCollection {

  //ObjectID를 쓰는 이유.
  //하나의 database를 가지는 RDB와는 달리
  //분산해서 저장하는 것을 목표로 하기 때문에
  // ObjectId를 생성해서 처리하게끔 한다
  // ObjectId는 중복될 확률이 매우 낮다.
  @Id
  private ObjectId id;

  private String firstname, lastname;


  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }
}
