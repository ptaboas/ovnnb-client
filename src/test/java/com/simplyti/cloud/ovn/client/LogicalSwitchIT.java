package com.simplyti.cloud.ovn.client;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;


public class LogicalSwitchIT {
	
	private static final Map<String,String> TEST_ENTRY_ID = Collections.singletonMap("testing", "true");
	
	private OVNNbClient client;
	
	@Before
	public void createClient() throws InterruptedException {
		NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
		
		this.client = OVNNbClient.builder()
				.eventLoop(eventLoopGroup)
				.server("localhost",6641)
				.verbose(true)
				.build();
		this.client.deleteLogicalSwitchs(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
	}
	
	@After
	public void desrtoyClient() throws InterruptedException{
		client.close();
		this.client.deleteLogicalSwitchs(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
	}
	
	@Test
	public void createSwitch() throws InterruptedException{
		Future<UUID> createResult = client.createLogicalSwitch("test",TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		Future<LogicalSwitch> futureSwtich = client.getLogicalSwitch("test").await();
		assertThat(futureSwtich.getNow(),notNullValue());
		assertThat(futureSwtich.getNow().getUuid(),equalTo(createResult.getNow()));
		assertThat(futureSwtich.getNow().getName(),equalTo("test"));
	}
	
	@Test
	public void createAnExistingSwitch() throws InterruptedException{
		client.createLogicalSwitch("test",TEST_ENTRY_ID).await();

		Future<UUID> result = client.createLogicalSwitch("test",TEST_ENTRY_ID).await();
		assertFalse(result.isSuccess());
		assertThat(result.cause(),instanceOf(IllegalStateException.class));
		assertThat(result.cause().getMessage(),equalTo("Logical switch test already exists"));
	}
	
	@Test
	public void deleteSwitchByname() throws InterruptedException{
		client.createLogicalSwitch("test").await();
		
		Future<Void> deleteResult = client.deleteLogicalSwitch("test").await();
		assertTrue(deleteResult.isSuccess());
		
		Future<LogicalSwitch> result = client.getLogicalSwitch("test").await();
		assertThat(result.getNow(),nullValue());
	}

}
