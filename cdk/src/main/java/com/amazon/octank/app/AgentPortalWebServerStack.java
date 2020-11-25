package com.amazon.octank.app;

import com.amazon.octank.network.NetworkStack;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalWebServerStack extends Stack {

	public AgentPortalWebServerStack(
		final Construct scope, final String id, final StackProps props, final NetworkStack networkStack,
		final AgentPortalStack agentPortalStack) {

		super(scope, id, props);

		_agentPortalWebServerConstruct = new AgentPortalWebServerConstruct(
			this, "AgentPortalWebServers", networkStack, agentPortalStack.getAgentPortalAppServer());
	}

	public AgentPortalWebServerConstruct getAgentPortalWebServerConstruct() {
		return _agentPortalWebServerConstruct;
	}

	private final AgentPortalWebServerConstruct _agentPortalWebServerConstruct;

}