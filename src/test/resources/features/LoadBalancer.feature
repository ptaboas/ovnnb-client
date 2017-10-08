Feature: Load Balancer operations

Background: No switch/router/loadbalancer exist
	Given there not exist any logical switch
	Given there not exist any logical router
	And there not exist any load balancer

Scenario: Create Load balancer attached to switch
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has protocol "TCP"
	And I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create Load balancer and then attach it to switch name
	And I create a load balancer with name "lb", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080"
	Then I check that exist a load balancer with name "lb"
	When I create a logical switch with name "sw" as "#sw"
	And I get the load balancer with name "lb" as "#lb"
	And I attach load balancer "#lb" to switch "#sw"
	And I get the logical switch with name "sw" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create Load balancer and then update switch attaching load balancer
	And I create a load balancer with name "lb", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080"
	Then I check that exist a load balancer with name "lb"
	When I create a logical switch with name "sw" as "#sw"
	And I get the load balancer with name "lb" as "#lb"
	And I update switch "sw" attaching load balancer "#lb"
	And I get the logical switch with name "sw" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"
	
Scenario: Create Load balancer attached to router
	When I create a logical router with name "router"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical router "router"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has protocol "TCP"
	And I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I get the logical router with name "router" as "#lr"
	Then I check that logical router "#lr" has 1 load balancer
	And I check that logical router "#lr" contains load balancer "#lb"
	
Scenario: I cannot create a load balancer with already existing name
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080"
	When I try to create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080" attached to logical switch "switch" getting error "#error"
	Then I check that error "#error" is instance of "com.simplyti.cloud.ovn.client.exception.OvnException"
	And I check that error "#error" contains message "Resource with name lb1 already exist"
	
Scenario: Create Load balancer with no ports
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1", targets "192.168.1.1,192.168.1.2" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has protocol "TCP"
	And I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1" with targets "192.168.1.1,192.168.1.2"
	When I get the logical switch with name "switch" as "#ls"
	Then I check that logical switch "#ls" has 1 load balancer
	And I check that logical switch "#ls" contains load balancer "#lb"

Scenario: Add and delete Load balancer virtual ip 
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	When I add virtual ip "10.0.0.2:8080" with targets "192.168.2.1:8080,192.168.2.2:8080" to load balancer "#lb"
	And I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 2 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	And I check that load balancer "#lb" contains virtual ip "10.0.0.2:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
	When I delete "TCP" virtual ip "10.0.0.2:8080" of load balancer "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	
Scenario: Add and delete target of a virtual ip
	And I create a load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I add targets "192.168.1.3:8080,192.168.1.4:8080" to virtual ip "10.0.0.1:8080" in load balancer "#lb"
	And I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080,192.168.1.3:8080,192.168.1.4:8080"
	When I delete targets "192.168.1.1:8080,192.168.1.3:8080" from virtual ip "10.0.0.1:8080" in load balancer "#lb"
	And I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.2:8080,192.168.1.4:8080"
	
Scenario: Delete Load balancer by created name attached to switch
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I delete load balancer with name "lb1" with forced option
	Then I check that does not exist load balancer with name "lb1"
	
Scenario: Delete Load balancer by created name attached to router
	When I create a logical router with name "router"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical router "router"
	Then I check that exist a load balancer with name "lb1"
	When I delete load balancer with name "lb1" with forced option
	Then I check that does not exist load balancer with name "lb1"
	
Scenario: Delete multiple load balancers
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" and external ids "testing=true" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I create a "TCP" load balancer with name "lb2", virtual ip "10.0.0.2:8080", targets "192.168.2.1:8080,192.168.2.2:8080" and external ids "testing=true" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb2"
	When I delete load balancers with external ids "testing=true" with forced option
	Then I check that exist 0 load balancers
	
Scenario: Delete Load balancer attached to switch with not force option should return an error
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I try to delete load balancer with name "lb1" getting promise "#result"
	Then I check that result promise "#result" has failed
	And I check that result ptomise "#result" has failure message that match with "referential integrity violation: cannot delete Load_Balancer row .*"
	
Scenario: Update unexsisting load balancer
	When I try to update load balancer "lb" adding virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080" getting result "#result"
	Then I check that result promise "#result" has failed
	And I check that result ptomise "#result" has failure message that match with "Resource with name lb doesn't exist"
	
Scenario: Update Load balancer virtual ip setting new targets
	When I create a logical switch with name "switch"
	And I create a "TCP" load balancer with name "lb1", virtual ip "10.0.0.1:8080", targets "192.168.1.1:8080,192.168.1.2:8080" attached to logical switch "switch"
	Then I check that exist a load balancer with name "lb1"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.1.1:8080,192.168.1.2:8080"
	When I set virtual ip "10.0.0.1:8080" targets to "192.168.2.1:8080,192.168.2.2:8080" to load balancer "#lb"
	When I get the load balancer with name "lb1" as "#lb"
	Then I check that load balancer "#lb" has 1 virtual ips
	And I check that load balancer "#lb" contains virtual ip "10.0.0.1:8080" with targets "192.168.2.1:8080,192.168.2.2:8080"
