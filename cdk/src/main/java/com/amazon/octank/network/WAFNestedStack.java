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

package com.amazon.octank.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.NestedStack;
import software.amazon.awscdk.core.NestedStackProps;
import software.amazon.awscdk.services.wafv2.CfnWebACL;
import software.amazon.awscdk.services.wafv2.CfnWebACLProps;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael C. Han (mhnmz)
 */
public class WAFNestedStack extends NestedStack {

	public WAFNestedStack(@NotNull Construct scope, @NotNull String id) {
		this(scope, id, null);
	}

	public WAFNestedStack(@NotNull Construct scope, @NotNull String id, @Nullable NestedStackProps props) {
		super(scope, id, props);

		_cfnWebACL = new CfnWebACL(this, "OctankAgentPortalWaf", new CfnWebACLProps() {
			@Override
			public @NotNull Object getDefaultAction() {
				return CfnWebACL.DefaultActionProperty.builder().allow(Boolean.TRUE).build();
			}

			@Override
			public @NotNull String getScope() {
				return "REGIONAL";
			}

			@Override
			public @NotNull Object getVisibilityConfig() {
				return CfnWebACL.VisibilityConfigProperty.builder().cloudWatchMetricsEnabled(true)
				                                         .sampledRequestsEnabled(true).metricName("AgentPortalWaf")
				                                         .build();
			}

			@Override
			public String getName() {
				return "OctankAgentPortalRegionalWaf";
			}
		});

		List<Object> rules = new ArrayList<>();

		//CfnRuleGroupProps cfnRuleGroupProps = CfnRuleGroupProps.builder().;

		//_cfnWebACL.getRules(rules);
	}

	public CfnWebACL getCfnWebACL() {
		return _cfnWebACL;
	}

	private CfnWebACL _cfnWebACL;

}
