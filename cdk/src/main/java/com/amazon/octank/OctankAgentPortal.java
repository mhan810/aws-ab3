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

import com.amazon.octank.app.AgentPortalStack;
import com.amazon.octank.app.AgentPortalWebServerStack;
import com.amazon.octank.db.AgentPortalDBStack;
import com.amazon.octank.network.BastionStack;
import com.amazon.octank.network.NetworkStack;
import com.amazon.octank.security.EncryptionKeyStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tags;

/**
 * @author Michael C. Han (mhnmz)
 */
public class OctankAgentPortal extends Construct {

	public static final String DEPLOYMENT_REGION = "us-east-1";
	public static final String KEY_PAIR_NAME = "ab3-key-pair";

	protected OctankAgentPortal(Construct scope, String id, Environment environment) {
		super(scope, id);

		software.amazon.awscdk.core.Environment env = software.amazon.awscdk.core.Environment.builder().region(
			DEPLOYMENT_REGION).build();

		StackProps stackProps = StackProps.builder().env(env).build();

		NetworkStack networkStack = new NetworkStack(this, "OctankNetwork", stackProps);

		BastionStack bastionStack = new BastionStack(this, "OctankBastion", stackProps, networkStack);

		EncryptionKeyStack encryptionKeyStack = new EncryptionKeyStack(this, "OctankKeys", stackProps, environment);

		// @todo private CAs not supported well in CDK 1.74 can re-examine when support improved
		//CertificateStack certificateStack = new CertificateStack(this, "OctankCA", stackProps);

		AgentPortalDBStack agentPortalDBStack = new AgentPortalDBStack(
			this, "OctankDb", stackProps, networkStack, encryptionKeyStack, environment);

		AgentPortalStack agentPortalStack = new AgentPortalStack(this, "OctankAgentPortal", stackProps, networkStack);

		AgentPortalWebServerStack agentPortalWebServerStack = new AgentPortalWebServerStack(
			this, "OctankWebServer", stackProps, networkStack, agentPortalStack);
		//WAFNestedStack wafNestedStack = new WAFNestedStack(networkStack, "OctankWAF", stackProps);

		Tags.of(networkStack).add("project", "AB3");
		Tags.of(encryptionKeyStack).add("project", "AB3");
		Tags.of(bastionStack).add("project", "AB3");
		//Tags.of(certificateStack).add("project", "AB3");
		Tags.of(agentPortalDBStack).add("project", "AB3");
		Tags.of(agentPortalStack).add("project", "AB3");
		Tags.of(agentPortalWebServerStack).add("project", "AB3");
		//Tags.of(wafNestedStack).add("project", "AB3");
	}

	public static void main(final String[] args) {
		App app = new App();

		Environment environment = Environment.NON_PRODUCTION;

		String envPropValue = System.getenv("env");

		if ((envPropValue != null) && (envPropValue.equals("prd") || envPropValue.equals("prod"))) {
			environment = Environment.PRODUCTION;
		}

		new OctankAgentPortal(app, "poc", environment);

		app.synth();
	}
}
