package study.ms.reactive.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AwsSqsConfig {

  @Value("${awsproperty.accesskeyid}")
  String accessKeyId ;
  @Value("${awsproperty.secretaccesskey}")
  String secretAccessKey;
  @Bean
  public AwsCredentials awsCredentials(){
    return AwsBasicCredentials.create(
        accessKeyId,
        secretAccessKey);
  }

  @Bean
  public SqsAsyncClient sqsClient(){
   return  SqsAsyncClient.builder()
        .region(Region.AP_NORTHEAST_2)
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials()))
        .build();
  }

}
