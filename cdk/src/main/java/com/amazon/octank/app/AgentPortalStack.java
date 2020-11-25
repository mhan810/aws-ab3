/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.octank.app;

import com.amazon.octank.network.NetworkStack;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalStack extends Stack {

	public AgentPortalStack(
		final Construct scope, final String id, final StackProps props, final NetworkStack networkStack) {

		super(scope, id, props);

	    _agentPortalAppServer = new AgentPortalAppServerConstruct(
			this, "AgentPortalAppServers", networkStack);
	}

	public AgentPortalAppServerConstruct getAgentPortalAppServer() {
		return _agentPortalAppServer;
	}

	private final AgentPortalAppServerConstruct _agentPortalAppServer;

}
