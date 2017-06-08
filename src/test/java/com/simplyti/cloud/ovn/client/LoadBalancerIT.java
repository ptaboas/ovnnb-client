package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Address;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;

import io.netty.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.contains;

public class LoadBalancerIT {
	
	private static final Map<String,String> TEST_ENTRY_ID = Collections.singletonMap("testing", "true");
	
	private OVNNbClient client;
	
	@Before
	public void createClient() throws InterruptedException {
		this.client = OVNNbClient.builder()
				.server("localhost",6641)
				.verbose()
				.build();
		this.client.deleteLogicalSwitchs(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
		this.client.deleteLoadBalancers(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
	}
	
	@After
	public void desrtoyClient() throws InterruptedException{
		client.close();
		this.client.deleteLogicalSwitchs(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
		this.client.deleteLoadBalancers(Criteria.field("external_ids").includes(TEST_ENTRY_ID)).sync();
	}
	
	@Test
	public void createLoadBalancer() throws InterruptedException{
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> createResult = client.createLoadBalancer("test",vip,ips,TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		Future<LoadBalancer> futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow(),notNullValue());
		assertThat(futureLoadBalancer.getNow().getUuid(),equalTo(createResult.getNow()));
		assertThat(futureLoadBalancer.getNow().getName(),equalTo("test"));
	}
	
	@Test
	public void createAnExistingLoadBalancer() throws InterruptedException{
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		client.createLoadBalancer("test",vip,ips,TEST_ENTRY_ID).await();

		Future<UUID> result = client.createLoadBalancer("test",vip,ips,TEST_ENTRY_ID).await();
		assertFalse(result.isSuccess());
		assertThat(result.cause(),instanceOf(IllegalStateException.class));
		assertThat(result.cause().getMessage(),equalTo("Load balancer test already exists"));
	}
	
	@Test
	public void deleteLoadBalancerByName() throws InterruptedException{
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		client.createLoadBalancer("test",vip,ips,TEST_ENTRY_ID).await();
		
		Future<Void> deleteResult = client.deleteLoadBalancer("test").await();
		assertTrue(deleteResult.isSuccess());
		
		Future<LoadBalancer> result = client.getLoadBalancer("test").await();
		assertThat(result.getNow(),nullValue());
	}
	
	@Test
	public void attachLoadBalancerToLogicalSwitch() throws InterruptedException{
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> lb = client.createLoadBalancer("lb1",vip,ips,TEST_ENTRY_ID).await();
		client.createLogicalSwitch("switch1",TEST_ENTRY_ID).await();
		
		Future<Void> result = client.attachLoadBalancerToSwitch("lb1","switch1").await();
		assertTrue(result.isSuccess());
		
		Future<LogicalSwitch> modifiedLb = client.getLogicalSwitch("switch1").await();
		assertThat(modifiedLb.getNow().getLoadBalancers(),contains(lb.getNow()));
	}
	
}
