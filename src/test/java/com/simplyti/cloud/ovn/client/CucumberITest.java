package com.simplyti.cloud.ovn.client;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;
import cucumber.api.junit.Cucumber;
import io.netty.channel.nio.NioEventLoopGroup;

@RunWith(Cucumber.class)
@CucumberOptions(
		features="classpath:features",
		snippets=SnippetType.CAMELCASE,
		plugin="pretty"
		)
public class CucumberITest {
	
	@BeforeClass
	public static void health(){
		OVNNbClient client = OVNNbClient.builder()
				.eventLoop(new NioEventLoopGroup())
				.server("localhost",6641)
				.build();
		
		await().pollInterval(100, TimeUnit.MILLISECONDS).atMost(5,TimeUnit.MINUTES)
		.until(()->client.dbs().await().isSuccess());
	}
}
