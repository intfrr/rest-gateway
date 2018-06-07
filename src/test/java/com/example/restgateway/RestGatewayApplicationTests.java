package com.example.restgateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestGatewayApplicationTests {

	@ClassRule
	public static KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(1, true);

	@Autowired
	private TestRestTemplate testRestTemplate;

	@BeforeClass
	public static void setup() {
		System.setProperty("spring.kafka.bootstrap-servers", kafkaEmbedded.getBrokersAsString());
	}

	@Test
	public void testSpringCloudStreamGateway() {
		assertThat(this.testRestTemplate.getForObject("/string/{string}", String.class, "foo")).isEqualTo("FOO");
		assertThat(this.testRestTemplate.getForObject("/string/{string}", String.class, "bar")).isEqualTo("BAR");
		assertThat(this.testRestTemplate.getForObject("/string/{string}", String.class, "baz")).isEqualTo("BAZ");
	}

}
