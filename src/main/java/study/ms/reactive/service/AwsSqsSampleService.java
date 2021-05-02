package study.ms.reactive.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Service
public class AwsSqsSampleService {

  @Autowired
  SqsAsyncClient sqsAsyncClient;


  public void sendSamplesMessage(SqsClient sqsClient, String queueName, String message) {

    try {
      CreateQueueRequest request = CreateQueueRequest.builder()
          .queueName(queueName)
          .build();
      CreateQueueResponse createResult = sqsClient.createQueue(request);

      GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
          .queueName(queueName)
          .build();

      String queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();

      SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
          .queueUrl(queueUrl)
          .messageBody(message)
          .delaySeconds(5)
          .build();
      SendMessageResponse sendMessageResponse = sqsClient.sendMessage(sendMsgRequest);

      ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
          .queueUrl(queueUrl)
          .build();
      List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

    } catch (SqsException e) {
      System.err.println("error ~~~!!!" + e.awsErrorDetails().errorMessage());
    }
  }


  public String asyncreceiveSampleMessageSample(SqsAsyncClient sqsAsyncClient, String queueName) {

    String queryUrl = null;
    try {
      queryUrl = sqsAsyncClient
          .getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).get().queueUrl();

      return sqsAsyncClient
          .receiveMessage(
              ReceiveMessageRequest.builder()
                  .maxNumberOfMessages(5)
                  .queueUrl(queryUrl)
                  .waitTimeSeconds(10)
                  .visibilityTimeout(30)
                  .build()
          ).get().messages().get(0).body();
    } catch (InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException();
    } catch (ExecutionException e) {
      e.printStackTrace();
      throw new RuntimeException();
    }
  }

  //TODO 성공 응답이나, 실패 응답, 적용 필요.
  //큐 실패 된 거는 어떻게 처리?
  //응답도 불가능한데 그냥 실패처리?
  public Mono<SendMessageResponse> asyncSendSampleMessageReturnMono(String queueName,
      String message) {

      GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
          .queueName(queueName)
          .build();

      return Mono.fromFuture(sqsAsyncClient.getQueueUrl(getQueueRequest)).flatMap(
          (o) -> {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(o.queueUrl())
                .messageBody(message)
                .delaySeconds(5)
                .build();

            return Mono.fromFuture(sqsAsyncClient.sendMessage(sendMsgRequest));
          }
      );
    }

  public Mono<ReceiveMessageResponse> asyncReceiveSampleMessageReturnMono(String queueName) {

      return Mono.fromFuture(
          sqsAsyncClient
              .getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build())
      ).flatMap((o)-> {

        CompletableFuture<ReceiveMessageResponse> receiveMessageResponse = sqsAsyncClient
            .receiveMessage(
                ReceiveMessageRequest.builder()
                    .maxNumberOfMessages(5)
                    .queueUrl(o.queueUrl())
                    .waitTimeSeconds(10)
                    .visibilityTimeout(30)
                    .build()
            );
        return Mono.fromFuture(receiveMessageResponse);
      });
  }



}
