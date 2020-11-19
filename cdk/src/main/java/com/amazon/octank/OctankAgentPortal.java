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

package com.amazon.octank;

import com.amazon.octank.network.NetworkStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;

/**
 * @author Michael C. Han (mhnmz)
 */
public class OctankAgentPortal extends Construct {

	protected OctankAgentPortal(Construct scope, String id) {
		super(scope, id);

		NetworkStack networkStack = new NetworkStack(this, "OctankNetwork");

		SecurityGroup bastionSecurityGroup = networkStack.getSecurityGroups().get(
			NetworkStack.BASTION_SG_ID);

		bastionSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22));

		//WAFNestedStack wafNestedStack = new WAFNestedStack(networkStack, "OctankWAF");

		Tags.of(networkStack).add("project", "AB3");
		//Tags.of(wafNestedStack).add("project", "AB3");
	}

	public static void main(final String[] args) {
		App app = new App();

		new OctankAgentPortal(app, "poc");

		app.synth();
	}
}