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

import com.amazon.octank.network.NetworkStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseInstanceProps;
import software.amazon.awscdk.services.rds.SqlServerEngineVersion;
import software.amazon.awscdk.services.rds.SubnetGroup;
import software.amazon.awscdk.services.rds.SubnetGroupProps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalDBStack extends Stack {
	public AgentPortalDBStack(@NotNull Construct scope, @NotNull String id, @NotNull final NetworkStack networkStack) {
		this(scope, id, null, networkStack);
	}

	public AgentPortalDBStack(
		@NotNull Construct scope, @NotNull String id, @Nullable StackProps props,
		@NotNull final NetworkStack networkStack) {

		super(scope, id, props);

		Vpc vpc = networkStack.getVpc();

		DatabaseInstanceProps.Builder databaseInstancePropsBuilder = DatabaseInstanceProps.builder().vpc(vpc).engine(
			DatabaseInstanceEngine.sqlServerSe(() -> SqlServerEngineVersion.VER_15_00_4043_16_V1));

		databaseInstancePropsBuilder.availabilityZone("us-east-1a");

		//@todo make sure multi-AZ configured w/ right SQLServer engine
		databaseInstancePropsBuilder.multiAz(true);

		databaseInstancePropsBuilder.allocatedStorage(20);
		databaseInstancePropsBuilder.autoMinorVersionUpgrade(true).backupRetention(Duration.days(7));

		//@todo use KMS to encrypt password
		databaseInstancePropsBuilder.credentials(Credentials.fromGeneratedSecret("admin"));

		//databaseInstancePropsBuilder.cloudwatchLogsExports();
		databaseInstancePropsBuilder.databaseName("Octank");
		databaseInstancePropsBuilder.enablePerformanceInsights(true);
		databaseInstancePropsBuilder.instanceIdentifier("octank");

		databaseInstancePropsBuilder.storageEncrypted(true);
		//@todo KMS key for encryption
		//databaseInstancePropsBuilder.storageEncryptionKey();

		databaseInstancePropsBuilder.securityGroups(
			Collections.singletonList(networkStack.getSecurityGroups().get(NetworkStack.DB_SG_ID)));

		List<ISubnet> dbSubnets = new ArrayList<>();

		vpc.getPrivateSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.DB_SUBNET_NAME)) {
				dbSubnets.add(iSubnet);
			}
		});

		SubnetGroupProps.Builder subnetGroupPropsBuilder = SubnetGroupProps.builder().description(
			"Subnet Group for AgentPortalDb").subnetGroupName(NetworkStack.DB_SUBNET_NAME);

		subnetGroupPropsBuilder.vpc(networkStack.getVpc()).vpcSubnets(
			SubnetSelection.builder().subnets(dbSubnets).build());

		databaseInstancePropsBuilder.subnetGroup(
			new SubnetGroup(this, "AgentPortalDbSubnetGroup", subnetGroupPropsBuilder.build()));

		_databaseInstance = new DatabaseInstance(this, "AgentPortalDB", databaseInstancePropsBuilder.build());
	}

	private final DatabaseInstance _databaseInstance;

}
