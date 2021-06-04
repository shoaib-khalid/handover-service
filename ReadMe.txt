++++++++++++++++++++++++++++++++
+ handover-service 1.1-SNAPSHOT
++++++++++++++++++++++++++++++++

Flow:
1. Handover service receives inbound messages from channel wrappers. 
2. It validates and verifies the incoming message and forwards message to live agents (if any one is available)
    - Creates visitor using rocket chat api
    - Creates room using rocket chat api
    - Queries storeId from flow-core API against referenceId
	- Queries agent information from user-service API against storeId
		Chooses random agent if store has >1 agent. If no agent is found, default agent will be used
    - send message for agent using rocket chat api
3. It receives outbound messages from live agent and forwards to respective wrapper (wrapper url is read from custom fields of RC api)
4. Dangling chat handling

++++++++++++++++++++++++++++++++
+ handover-service 0.0.1-SNAPSHOT
++++++++++++++++++++++++++++++++

Flow:
1. Handover service receives inbound messages from channel wrappers. 
2. It validates and verifies the incoming message and forwards message to live agents (if any one is available)
    - Creates visitor using rocket chat api
    - Creates room using rocket chat api
    - send message for agent using rocket chat api
3. It receives outbound messages from live agent and forwards to respective wrapper (wrapper url is read from custom fields of RC api)
4. It also periodically checks agents availability. Forwards offline message if supported by agents interface