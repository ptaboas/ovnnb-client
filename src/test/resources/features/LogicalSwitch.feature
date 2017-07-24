Feature: Logical Switch operations

Background: No ligical switch exist
	Given there not exist any logical switch

Scenario: Create Logical switch
	When I create a logical switch with name "test"
	Then I check that exist a logical switch with name "test"
	
Scenario: Create Logical switch witch external ids
	When I create a logical switch with name "test" and external ids "testing=true"
	Then I check that exist a logical switch with name "test"
	When I get the logical switch with name "test" as "#ls"
	Then I check that logical switch "#ls" contains external ids "testing=true"
	
Scenario: Create an existing logical switch should be idempotent
	When I create a logical switch with name "test" getting the created uuid "#uuid"
	And I create a logical switch with name "test" getting the created uuid "#existinguuid"
	Then I check that uuid "#existinguuid" is equals to "#uuid"
	
Scenario: Delete Logical switch
	When I create a logical switch with name "test"
	Then I check that exist a logical switch with name "test"
	When I delete the logical switch with name "test"
	Then I check that does not exist logical switch with name "test"
	
Scenario: Delete multiple logical switch using custom criteria
	When I create a logical switch with name "switch1" and external ids "testing=true"
	And I create a logical switch with name "switch2" and external ids "testing=true"
	Then I check that exist 2 logical switches
	When I delete logical switches with external ids "testing=true"
	Then I check that exist 0 logical switches
	
Scenario: Close client shoud close channels, but I should be able to continue using the client as normal
	When I create a logical switch with name "test"
	And I close ovn northbound client
	Then I check that exist a logical switch with name "test"