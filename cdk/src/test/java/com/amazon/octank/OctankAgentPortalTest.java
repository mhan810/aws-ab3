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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.cxapi.CloudAssembly;

/**
 * @author Michael C. Han (mhnmz)
 */
public class OctankAgentPortalTest {

	@Test
	public void testStack() {
		App app = new App();

		OctankAgentPortal octankAgentPortal = new OctankAgentPortal(app, "test");

		// synthesize the stack to a CloudFormation template and compare against
		// a checked-in JSON file.
		CloudAssembly cloudAssembly = app.synth();

		cloudAssembly.getStacks().forEach(cloudFormationStackArtifact -> System.out.println(
			JSON.valueToTree(cloudFormationStackArtifact.getTemplate()).toPrettyString()));

		//assertThat(new ObjectMapper().createObjectNode()).isEqualTo(actual);
	}

	private final static ObjectMapper JSON = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
}
