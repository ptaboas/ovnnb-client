package com.simplyti.cloud.ovn.client;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(
		features="classpath:features",
		snippets=SnippetType.CAMELCASE)
public class CucumberITest {}
