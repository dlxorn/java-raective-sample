package study.ms.reactive.collection;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sample_web_client")
public class SampleWebClientCollection {

  @Id
  private ObjectId objectId ;

  private Long userId;
  private Long id;
  private String title;
  private Boolean completed;

  public ObjectId getObjectId() {
    return objectId;
  }

  public void setObjectId(ObjectId objectId) {
    this.objectId = objectId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getCompleted() {
    return completed;
  }

  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }
}
