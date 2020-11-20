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

import com.amazon.octank.Environment;
import com.amazon.octank.network.NetworkStack;
import com.amazon.octank.security.EncryptionKeyStack;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.DatabaseInstanceProps;
import software.amazon.awscdk.services.rds.DatabaseSecret;
import software.amazon.awscdk.services.rds.DatabaseSecretProps;
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

	//
	public AgentPortalDBStack(
		final Construct scope, final String id, final StackProps stackProps, final NetworkStack networkStack,
		final EncryptionKeyStack encryptionKeyStack, Environment environment) {

		super(scope, id, stackProps);

		Vpc vpc = networkStack.getVpc();

		DatabaseInstanceProps.Builder databaseInstancePropsBuilder = DatabaseInstanceProps.builder().availabilityZone(
			"us-east-1a").vpc(vpc);

		if (environment.equals(Environment.PRODUCTION)) {
			databaseInstancePropsBuilder.engine(
				DatabaseInstanceEngine.sqlServerSe(() -> SqlServerEngineVersion.VER_15_00_4043_16_V1));

			databaseInstancePropsBuilder.instanceType(InstanceType.of(InstanceClass.STANDARD5, InstanceSize.XLARGE));
			databaseInstancePropsBuilder.multiAz(true);
		}
		else {
			//non-prod
			databaseInstancePropsBuilder.engine(
				DatabaseInstanceEngine.sqlServerWeb(() -> SqlServerEngineVersion.VER_15_00_4043_16_V1));

			databaseInstancePropsBuilder.instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.LARGE));
			databaseInstancePropsBuilder.multiAz(false);
		}
		databaseInstancePropsBuilder.allocatedStorage(20);

		databaseInstancePropsBuilder.autoMinorVersionUpgrade(true).backupRetention(Duration.days(7));

		addCredentials(encryptionKeyStack, databaseInstancePropsBuilder);

		databaseInstancePropsBuilder.cloudwatchLogsExports(Collections.singletonList("error"));
		databaseInstancePropsBuilder.enablePerformanceInsights(true);
		databaseInstancePropsBuilder.instanceIdentifier("octank-1");
		databaseInstancePropsBuilder.deleteAutomatedBackups(true);

		databaseInstancePropsBuilder.maxAllocatedStorage(100);
		databaseInstancePropsBuilder.storageEncrypted(true);
		databaseInstancePropsBuilder.storageEncryptionKey(encryptionKeyStack.getDataEncryptionKey());

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

		_databaseInstance = new DatabaseInstance(this, id + "AgentPortalDB", databaseInstancePropsBuilder.build());
	}

	public DatabaseInstance getDatabaseInstance() {
		return _databaseInstance;
	}

	private void addCredentials(
		EncryptionKeyStack encryptionKeyStack, DatabaseInstanceProps.Builder databaseInstancePropsBuilder) {

		DatabaseSecretProps databaseSecretProps = DatabaseSecretProps.builder().username("admin").encryptionKey(
			encryptionKeyStack.getPassEncryptionKey()).build();

		DatabaseSecret databaseSecret = new DatabaseSecret(this, "OctankDatabaseSecret", databaseSecretProps);

		databaseInstancePropsBuilder.credentials(Credentials.fromSecret(databaseSecret, "admin"));
	}

	private final DatabaseInstance _databaseInstance;

}
