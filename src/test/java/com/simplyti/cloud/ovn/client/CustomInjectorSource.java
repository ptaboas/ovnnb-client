package com.simplyti.cloud.ovn.client;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import cucumber.runtime.java.guice.InjectorSource;
import cucumber.runtime.java.guice.ScenarioScoped;
import cucumber.runtime.java.guice.impl.ScenarioModule;
import cucumber.runtime.java.guice.impl.SequentialScenarioScope;
import io.netty.channel.nio.NioEventLoopGroup;

public class CustomInjectorSource extends AbstractModule implements InjectorSource{

	@Override
	public Injector getInjector() {
		 ScenarioModule scenarioModule = new ScenarioModule(new SequentialScenarioScope());
         return Guice.createInjector(scenarioModule,this);
	}

	@Override
	protected void configure() {
		OVNNbClient client = OVNNbClient.builder()
				.eventLoop(new NioEventLoopGroup())
				.server("localhost",6641)
				.withLog4J2Logger()
				.build();
		
		bind(OVNNbClient.class).toInstance(client);
	}
	
	@Provides
	@ScenarioScoped
	public Map<String,Object> scenarioData(){
		return Maps.newHashMap();
	}

}
