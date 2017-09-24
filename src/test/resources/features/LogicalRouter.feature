Feature: Logical Router operations

Background: No logical router exist
	Given there not exist any logical router

Scenario: Create Logical router
	When I create a logical router with name "test"
	Then I check that exist a logical router with name "test"
	
Scenario: Create Logical router witch external ids
	When I create a logical router with name "test" and external ids "testing=true"
	Then I check that exist a logical router with name "test"
	When I get the logical router with name "test" as "#ls"
	Then I check that logical router "#ls" contains external ids "testing=true"
	
Scenario: Delete Logical router
	When I create a logical router with name "test"
	Then I check that exist a logical router with name "test"
	When I delete the logical router with name "test"
	Then I check that does not exist logical router with name "test"
	
Scenario: Delete multiple logical router using custom criteria
	When I create a logical router with name "switch1" and external ids "testing=true"
	And I create a logical router with name "switch2" and external ids "testing=true"
	Then I check that exist 2 logical routers
	When I delete logical routers with external ids "testing=true"
	Then I check that exist 0 logical routers