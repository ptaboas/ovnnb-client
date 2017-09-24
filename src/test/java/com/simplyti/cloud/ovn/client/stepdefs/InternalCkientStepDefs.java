package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.simplyti.cloud.ovn.client.OVNNbClient;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.netty.util.concurrent.Future;

public class InternalCkientStepDefs {
	
	@Inject
	private OVNNbClient client;
	
	@Inject
	private Map<String,Object> scenarioData;
	
	@When("^I get list of databases as \"([^\"]*)\"$")
	public void iGetListOfDatabasesAs(String key) throws Throwable {
		Future<List<String>> dbs =  client.dbs().await();
		assertTrue(dbs.isSuccess());
		assertThat(dbs.getNow(),notNullValue());
		scenarioData.put(key, dbs.getNow());
	}
	
	@When("^I get list of databases as \"([^\"]*)\" no waiting result$")
	public void iGetListOfDatabasesAsNoWaitingResult(String key) throws Throwable {
		Future<List<String>> dbsPromise =  client.dbs();
		scenarioData.put(key, dbsPromise);
	}
	
	@Then("^I check that \"([^\"]*)\" contains \"([^\"]*)\"$")
	public void iCheckThatContains(String key, List<String> expect) throws Throwable {
		Collection<?> data = (Collection<?>) scenarioData.get(key);
		assertThat(data,containsInAnyOrder(expect.stream().toArray(Object[]::new)));
	}
	
	@Then("^I check that promise \"([^\"]*)\" is success$")
	public void iCheckThatPromiseIsSuccess(String key) throws Throwable {
		Future<?> future = (Future<?>) scenarioData.get(key);
	    await().atMost(10,TimeUnit.SECONDS).until(()->future.isDone());
	    assertThat(future.isSuccess(),equalTo(true));
	}
	
	@Then("^I check that client has received an echo message$")
	public void iCheckThatClientHasReceivedAnEchoMessage() throws Throwable {
		await().atMost(10,TimeUnit.SECONDS).until(()->client.internalClient().lastEcho()!=null);
		;
	}

}
