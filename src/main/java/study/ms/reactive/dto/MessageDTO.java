package study.ms.reactive.dto;

import java.time.LocalDateTime;

public class MessageDTO {

  String message = LocalDateTime.now().toString();

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
