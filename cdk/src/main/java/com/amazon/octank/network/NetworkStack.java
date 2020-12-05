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

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.GatewayVpcEndpointAwsService;
import software.amazon.awscdk.services.ec2.GatewayVpcEndpointOptions;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class NetworkStack extends Stack {

	public static final String APP_SG_ID = "AppSG";
	public static final String APP_SUBNET_NAME = "App";
	public static final String BASTION_SG_ID = "BastionSG";
	public static final String LAMBDA_SG_ID = "LambdaSG";
	public static final String DB_SG_ID = "DbSG";
	public static final String DB_SUBNET_NAME = "Db";
	public static final String DMZ_SG_ID = "DmzSG";
	public static final String DMZ_SUBNET_NAME = "Dmz";

	public NetworkStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		_vpc = buildVPC(this, id + "Vpc");

		addSecurityGroups(_vpc);
	}

	public Map<String, SecurityGroup> getSecurityGroups() {
		return _securityGroups;
	}

	public Vpc getVpc() {
		return _vpc;
	}

	private void addSecurityGroups(final Vpc vpc) {

		SecurityGroup dbSecurityGroup = new SecurityGroup(vpc, DB_SG_ID, new SecurityGroupProps() {
			@Override
			public IVpc getVpc() {
				return vpc;
			}

			@Override
			public String getSecurityGroupName() {
				return "Db";
			}
		});

		_securityGroups.put(DB_SG_ID, dbSecurityGroup);

		SecurityGroup appSecurityGroup = new SecurityGroup(vpc, APP_SG_ID, new SecurityGroupProps() {
			@Override
			public IVpc getVpc() {
				return vpc;
			}

			@Override
			public String getSecurityGroupName() {
				return "App";
			}
		});

		_securityGroups.put(APP_SG_ID, appSecurityGroup);

		SecurityGroup dmzSecurityGroup = new SecurityGroup(vpc, DMZ_SG_ID, new SecurityGroupProps() {
			@Override
			public IVpc getVpc() {
				return vpc;
			}

			@Override
			public String getSecurityGroupName() {
				return "Dmz";
			}
		});

		_securityGroups.put(DMZ_SG_ID, dmzSecurityGroup);

		SecurityGroup bastionSecurityGroup = new SecurityGroup(vpc, BASTION_SG_ID, new SecurityGroupProps() {
			@Override
			public IVpc getVpc() {
				return vpc;
			}

			@Override
			public String getSecurityGroupName() {
				return "Bastion";
			}
		});

		_securityGroups.put(BASTION_SG_ID, bastionSecurityGroup);

		SecurityGroup lambdaGroup = new SecurityGroup(vpc, LAMBDA_SG_ID, new SecurityGroupProps() {
			@Override
			public IVpc getVpc() {
				return vpc;
			}

			@Override
			public String getSecurityGroupName() {
				return "Lambda";
			}
		});

		_securityGroups.put(LAMBDA_SG_ID, lambdaGroup);

		//database group
		dbSecurityGroup.addIngressRule(appSecurityGroup, Port.tcp(1433));
		dbSecurityGroup.addIngressRule(appSecurityGroup, Port.tcp(3306));
		dbSecurityGroup.addIngressRule(appSecurityGroup, Port.tcp(5432));
		dbSecurityGroup.addIngressRule(appSecurityGroup, Port.tcp(1521));
		dbSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(1433));
		dbSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(3306));
		dbSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(5432));
		dbSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(1521));

		//app server group
		appSecurityGroup.addIngressRule(dmzSecurityGroup, Port.tcp(443));
		appSecurityGroup.addIngressRule(dmzSecurityGroup, Port.tcp(80));
		appSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(22));

		//dmz server group
		dmzSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(443));
		dmzSecurityGroup.addIngressRule(Peer.anyIpv6(), Port.tcp(443));
		dmzSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80));
		dmzSecurityGroup.addIngressRule(Peer.anyIpv6(), Port.tcp(80));
		dmzSecurityGroup.addIngressRule(bastionSecurityGroup, Port.tcp(22));

		lambdaGroup.addEgressRule(lambdaGroup, Port.allTraffic());
	}

	private Vpc buildVPC(Construct scope, String id) {
		return new Vpc(scope, id, new VpcProps() {
			@Override
			public String getCidr() {
				return "10.0.0.0/16";
			}

			@Override
			public Map<String, GatewayVpcEndpointOptions> getGatewayEndpoints() {
				Map<String, GatewayVpcEndpointOptions> gatewayVpcEndpointOptions = new HashMap<>();

				List<SubnetSelection> subnetSelections = Collections.singletonList(
					SubnetSelection.builder().subnetGroupName(APP_SUBNET_NAME).build());

				GatewayVpcEndpointOptions s3GatewayVpcEndpointOptions = GatewayVpcEndpointOptions.builder().service(
					GatewayVpcEndpointAwsService.S3).subnets(subnetSelections).build();

				gatewayVpcEndpointOptions.put("OctankS3Gateway", s3GatewayVpcEndpointOptions);

				return gatewayVpcEndpointOptions;
			}

			@Override
			public Number getMaxAzs() {
				return 2;
			}

			@Override
			public List<SubnetConfiguration> getSubnetConfiguration() {
				List<SubnetConfiguration> subnetConfigurations = new ArrayList<>();

				subnetConfigurations.add(SubnetConfiguration.builder().name(DB_SUBNET_NAME).subnetType(
					SubnetType.PRIVATE).cidrMask(24).build());

				subnetConfigurations.add(SubnetConfiguration.builder().name(APP_SUBNET_NAME).subnetType(
					SubnetType.PRIVATE).cidrMask(24).build());

				subnetConfigurations.add(SubnetConfiguration.builder().name(DMZ_SUBNET_NAME).subnetType(
					SubnetType.PUBLIC).cidrMask(24).build());

				return subnetConfigurations;
			}
		});
	}

	private final Map<String, SecurityGroup> _securityGroups = new HashMap<>();
	private final Vpc _vpc;

}
