package study.ms.reactive;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.Disposable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import study.ms.reactive.service.AwsSqsSampleService;

@SpringBootTest
public class AwsSqsTests {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  AwsSqsSampleService awsSqsSampleService;

  @Value("${awsproperty.accesskeyid}")
  String accessKeyId ;
  @Value("${awsproperty.secretaccesskey}")
  String secretAccessKey;


  @Test
  void awsSqsTest(){
    AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
        accessKeyId,
        secretAccessKey);

    SqsClient sqsClient = SqsClient.builder()
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
        .build();

//        String queueName = args[0];
//        String message = args[1];

    awsSqsSampleService.sendSamplesMessage(sqsClient, "MyQueue", "message");
    sqsClient.close();


    SqsAsyncClient sqsAsyncClient = SqsAsyncClient.builder()
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
        .build();
    String result = awsSqsSampleService.asyncreceiveSampleMessageSample(sqsAsyncClient, "MyQueue");

    logger.info(result);
    sqsAsyncClient.close();

  }

  //aws SQS 사용시 바로바로 처리는 안되는 것처럼 보였다.
  //뭔가 더 확인헤야할 것들이 있을듯, 너무 느리다.
  @Test
  void awsSqsWithWebfluxTest() throws Exception{
    awsSqsSampleService.asyncSendSampleMessageReturnMono("queueName", "testMessage").subscribe();

    Thread.sleep(2000);

    Disposable dasda = awsSqsSampleService
        .asyncReceiveSampleMessageReturnMono("queueName")
        .doOnSuccess(o->{
          logger.info("메세지 결과다 : {}" ,o.messages().toString());
        }).subscribe();
    while(!dasda.isDisposed()){
      Thread.sleep(1000);
    }
  }


}
