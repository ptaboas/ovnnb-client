package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Address;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.empty;

public class LoadBalancerIT {
	
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
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> createResult = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,vip,ips,TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		Future<LoadBalancer> futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow(),notNullValue());
		assertThat(futureLoadBalancer.getNow().getUuid(),equalTo(createResult.getNow()));
		assertThat(futureLoadBalancer.getNow().getName(),equalTo("test"));
		assertThat(futureLoadBalancer.getNow().getProtocol(),equalTo(Protocol.TCP.getValue()));
		assertThat(futureLoadBalancer.getNow().getVips().entrySet(),hasSize(1));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80"));
		
		Future<LogicalSwitch> testLs = client.getLogicalSwitch("testLs").await();
		assertThat(testLs.getNow().getLoadBalancers(),contains(futureLoadBalancer.getNow().getUuid()));
	}
	
	@Test
	public void createVipInExistingLoadBalancer() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Future<UUID> createResult1 = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,new Address("192.168.1.9", 80),
				ImmutableSet.of(new Address("10.0.0.1", 80),new Address("10.0.0.2", 80)),TEST_ENTRY_ID).await();
		assertThat(createResult1.getNow(),notNullValue());
		
		Future<UUID> createResult2 = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,new Address("192.168.1.9", 443),
				ImmutableSet.of(new Address("10.0.0.1", 443),new Address("10.0.0.2", 443)),TEST_ENTRY_ID).await();
		assertThat(createResult2.getNow(),notNullValue());
		assertThat(createResult2.getNow(),equalTo(createResult1.getNow()));
		
		Future<LoadBalancer> futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow(),notNullValue());
		assertThat(futureLoadBalancer.getNow().getUuid(),equalTo(createResult1.getNow()));
		assertThat(futureLoadBalancer.getNow().getUuid(),equalTo(createResult2.getNow()));
		assertThat(futureLoadBalancer.getNow().getName(),equalTo("test"));
		assertThat(futureLoadBalancer.getNow().getProtocol(),equalTo(Protocol.TCP.getValue()));
		assertThat(futureLoadBalancer.getNow().getVips().entrySet(),hasSize(2));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80,10.0.0.2:80"));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:443","10.0.0.1:443,10.0.0.2:443"));
		
		Future<LogicalSwitch> testLs = client.getLogicalSwitch("testLs").await();
		assertThat(testLs.getNow().getLoadBalancers(),contains(futureLoadBalancer.getNow().getUuid()));
	}
	
	@Test
	public void cannotCreateAlreadyExistingLoadBalancerWithDifferentData() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		client.createLoadBalancer("test",Collections.singleton("test"),Protocol.TCP,vip,Collections.singleton(new Address("10.0.0.1", 80)),TEST_ENTRY_ID).await();

		Future<UUID> result = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.UDP,vip,Collections.singleton(new Address("10.0.0.2", 80)),TEST_ENTRY_ID).await();
		assertFalse(result.isSuccess());
		assertThat(result.cause(),instanceOf(IllegalStateException.class));
		assertThat(result.cause().getMessage(),equalTo("Load balancer test already exists with different data"));
	}
	
	@Test
	public void loadBalancerCreationWithSameDataShoulBeIdempotent() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		UUID uuid = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,vip,Collections.singleton(new Address("10.0.0.1", 80)),TEST_ENTRY_ID).await().getNow();

		Future<UUID> result = client.createLoadBalancer("test",Collections.singleton("test"),Protocol.TCP,vip,Collections.singleton(new Address("10.0.0.1", 80)),TEST_ENTRY_ID).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),equalTo(uuid));
		
		Future<LogicalSwitch> testLs = client.getLogicalSwitch("testLs").await();
		assertThat(testLs.getNow().getLoadBalancers(),contains(result.getNow()));
	}
	
	@Test
	public void deleteLoadBalancerByName() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,vip,ips,TEST_ENTRY_ID).await();
		
		Future<Void> deleteResult = client.deleteLoadBalancer("test").await();
		assertTrue(deleteResult.isSuccess());
		
		Future<LoadBalancer> result = client.getLoadBalancer("test").await();
		assertThat(result.getNow(),nullValue());
		
		Future<LogicalSwitch> testLs = client.getLogicalSwitch("testLs").await();
		assertThat(testLs.getNow().getLoadBalancers(),empty());
	}
	
	@Test
	public void deleteUnexistingLoadBalancerByName() throws InterruptedException{
		Future<Void> deleteResult = client.deleteLoadBalancer("test").await();
		assertTrue(deleteResult.isSuccess());
	}
	
	@Test
	public void attachLoadBalancerToLogicalSwitch() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> lb = client.createLoadBalancer("lb1",Collections.singleton("testLs"),Protocol.TCP,vip,ips,TEST_ENTRY_ID).await();
		client.createLogicalSwitch("switch1",TEST_ENTRY_ID).await();
		
		Future<Void> result = client.attachLoadBalancerToSwitch("lb1","switch1").await();
		assertTrue(result.isSuccess());
		
		Future<LogicalSwitch> modifiedLb = client.getLogicalSwitch("switch1").await();
		assertThat(modifiedLb.getNow().getLoadBalancers(),contains(lb.getNow()));
	}
	
	@Test
	public void dettachLoadBalancerToLogicalSwitch() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> lb = client.createLoadBalancer("lb1",Collections.singleton("testLs"),Protocol.TCP,vip,ips,TEST_ENTRY_ID).await();
		client.createLogicalSwitch("switch1",TEST_ENTRY_ID).await();
		
		client.attachLoadBalancerToSwitch("lb1","switch1").await();
		
		Future<Void> dettach = client.dettachLoadBalancer(lb.getNow()).await();
		assertTrue(dettach.isSuccess());
		
		Future<LogicalSwitch> testLs = client.getLogicalSwitch("testLs").await();
		assertThat(testLs.getNow().getLoadBalancers(),empty());
		
		Future<LogicalSwitch> switch1 = client.getLogicalSwitch("switch1").await();
		assertThat(switch1.getNow().getLoadBalancers(),empty());
	}
	
	@Test
	public void updateLoadBalancer() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Address vip = new Address("192.168.1.9", 80);
		Collection<Address> ips = Collections.singleton(new Address("10.0.0.1", 80));
		Future<UUID> createResult = client.createLoadBalancer("test",Collections.singleton("testLs"),Protocol.TCP,vip,ips,TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		Future<LoadBalancer> futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80"));
		
		Future<Void> updateResult = client.updateLoadBalancerVip(createResult.getNow(),vip,ImmutableSet.of(new Address("10.0.0.1", 80),new Address("10.0.0.2", 80))).await();
		assertTrue(updateResult.isSuccess());
		
		futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80,10.0.0.2:80"));
	}
	
	@Test
	public void deleteLoadBalancerVip() throws InterruptedException{
		this.client.createLogicalSwitch("testLs", TEST_ENTRY_ID);
		
		Future<UUID> createResult = client.createLoadBalancer("test",
				Collections.singleton("testLs"),Protocol.TCP,
				new Address("192.168.1.9", 80),
				Collections.singleton(new Address("10.0.0.1", 80)),
				TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		Future<LoadBalancer> futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow().getVips().size(),equalTo(1));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80"));
		
		createResult = client.createLoadBalancer("test",
				Collections.singleton("testLs"),Protocol.TCP,
				new Address("192.168.1.9", 443),
				Collections.singleton(new Address("10.0.0.1", 443)),
				TEST_ENTRY_ID).await();
		assertThat(createResult.getNow(),notNullValue());
		
		futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow().getVips().size(),equalTo(2));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:80","10.0.0.1:80"));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:443","10.0.0.1:443"));
		
		Future<Void> deleteResult = client.deleteLoadBalancerVip("test", new Address("192.168.1.9", 80)).await();
		assertTrue(deleteResult.isSuccess());
		
		futureLoadBalancer = client.getLoadBalancer("test").await();
		assertThat(futureLoadBalancer.getNow().getVips().size(),equalTo(1));
		assertThat(futureLoadBalancer.getNow().getVips(),hasEntry("192.168.1.9:443","10.0.0.1:443"));
	}
	
}
