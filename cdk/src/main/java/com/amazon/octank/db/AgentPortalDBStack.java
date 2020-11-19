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

package com.amazon.octank.db;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseInstanceProps;
import software.amazon.awscdk.services.rds.IInstanceEngine;
import software.amazon.awscdk.services.rds.SqlServerEngineVersion;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalDBStack extends Stack {
	public AgentPortalDBStack(@NotNull Construct scope, @NotNull String id, @NotNull final Vpc vpc) {
		this(scope, id, null, vpc);
	}

	public AgentPortalDBStack(
		@NotNull Construct scope, @NotNull String id, @Nullable StackProps props, @NotNull final Vpc vpc) {

		super(scope, id, props);

		DatabaseInstance dbInstance = new DatabaseInstance(this, "AgentPortalDB", new DatabaseInstanceProps() {
			@Override
			public @NotNull IInstanceEngine getEngine() {
				return DatabaseInstanceEngine.sqlServerSe(() -> SqlServerEngineVersion.VER_15_00_4043_16_V1);
			}

			@Override
			public @NotNull IVpc getVpc() {
				return vpc;
			}
		});

	}

}
