Feature: Logical Switch operations

Background: No logical switch exist
	Given there not exist any logical switch

Scenario: Create Logical switch
	When I create a logical switch with name "test"
	Then I check that exist a logical switch with name "test"
	
Scenario: Create Logical switch witch external ids
	When I create a logical switch with name "test" and external ids "testing=true"
	Then I check that exist a logical switch with name "test"
	When I get the logical switch with name "test" as "#ls"
	Then I check that logical switch "#ls" contains external ids "testing=true"
	
Scenario: Create an existing logical switch should return an error
	When I create a logical switch with name "test" getting the created uuid "#uuid"
	And I try to create a logical switch with name "test" getting error "#error"
	Then I check that error "#error" is instance of "com.simplyti.cloud.ovn.client.exception.OvnException"
	And I check that error "#error" contains message "Resource with name test already exist"
	
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
	
Scenario: Create Logical switch witch subnet property
	When I create a logical switch with name "test" and subnet "10.0.0.0/24"
	Then I check that exist a logical switch with name "test"
	When I get the logical switch with name "test" as "#ls"
	Then I check that logical switch "#ls" has subnet "10.0.0.0/24"