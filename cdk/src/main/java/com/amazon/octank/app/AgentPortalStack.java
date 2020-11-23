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

import com.amazon.octank.OctankAgentPortal;
import com.amazon.octank.network.NetworkStack;
import com.amazon.octank.util.IAMUtils;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.autoscaling.ElbHealthCheckOptions;
import software.amazon.awscdk.services.autoscaling.GroupMetrics;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationTargetGroupProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.IpAddressType;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCertificate;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.SslPolicy;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.iam.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalStack extends Stack {

	public static final String AGENT_PORTAL_IMAGE_ID = "ami-09b540032d6ca0048";

	public AgentPortalStack(
		final Construct scope, final String id, final StackProps props, final NetworkStack networkStack) {

		super(scope, id, props);

		_appServerASG = addAppServerASG(networkStack);
		_appServerALB = addAppServerALB(networkStack);
	}

	private ApplicationLoadBalancer addAppServerALB(final NetworkStack networkStack) {

		ApplicationLoadBalancerProps.Builder albPropsBuilder = ApplicationLoadBalancerProps.builder().loadBalancerName(
			"AgentPortalALB");

		albPropsBuilder.http2Enabled(true).internetFacing(true).ipAddressType(IpAddressType.IPV4);

		Vpc vpc = networkStack.getVpc();

		albPropsBuilder.vpc(vpc);

		List<ISubnet> dmzSubnets = new ArrayList<>();

		vpc.getPublicSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.DMZ_SUBNET_NAME)) {
				dmzSubnets.add(iSubnet);
			}
		});

		albPropsBuilder.vpcSubnets(SubnetSelection.builder().subnets(dmzSubnets).build());

		Map<String, SecurityGroup> securityGroups = networkStack.getSecurityGroups();
		albPropsBuilder.securityGroup(securityGroups.get(NetworkStack.DMZ_SG_ID));

		_appServerALB = new ApplicationLoadBalancer(this, "AgentAppServerALB", albPropsBuilder.build());

		ApplicationTargetGroupProps.Builder appServerATGPropsBuilder =
			ApplicationTargetGroupProps.builder().targetGroupName("AppServerATG").targetType(TargetType.INSTANCE);

		appServerATGPropsBuilder.healthCheck(HealthCheck.builder().protocol(Protocol.HTTPS).build());
		appServerATGPropsBuilder.protocol(ApplicationProtocol.HTTPS);
		appServerATGPropsBuilder.port(443);
		appServerATGPropsBuilder.vpc(vpc);
		appServerATGPropsBuilder.stickinessCookieDuration(Duration.minutes(60));

		ApplicationTargetGroup applicationTargetGroup = new ApplicationTargetGroup(
			this, "AgentPortalATG", appServerATGPropsBuilder.build());

		_appServerASG.attachToApplicationTargetGroup(applicationTargetGroup);

		List<ApplicationTargetGroup> applicationTargetGroups = Collections.singletonList(applicationTargetGroup);

		BaseApplicationListenerProps.Builder sslAppListenerPropsBuilder = BaseApplicationListenerProps.builder();
		sslAppListenerPropsBuilder.open(true);
		sslAppListenerPropsBuilder.protocol(ApplicationProtocol.HTTPS);
		sslAppListenerPropsBuilder.port(443);
		sslAppListenerPropsBuilder.sslPolicy(SslPolicy.RECOMMENDED);

		sslAppListenerPropsBuilder.defaultTargetGroups(applicationTargetGroups);

		ListenerCertificate listenerCertificate = ListenerCertificate.fromArn(
			"arn:aws:acm:us-east-1:817387504538:certificate/b4ae6329-de7c-4318-b18e-58b5b59bf786");

		sslAppListenerPropsBuilder.certificates(Collections.singletonList(listenerCertificate));

		_appServerALB.addListener("AgentPortalALBHttp", sslAppListenerPropsBuilder.build());

		return _appServerALB;
	}

	private AutoScalingGroup addAppServerASG(NetworkStack networkStack) {
		AutoScalingGroupProps.Builder appServerASGPropsBuilder = AutoScalingGroupProps.builder().autoScalingGroupName(
			"AgentAppServerASG");

		appServerASGPropsBuilder.instanceType(InstanceType.of(InstanceClass.STANDARD5, InstanceSize.XLARGE));
		appServerASGPropsBuilder.machineImage(
			MachineImage.genericLinux(Collections.singletonMap("us-east-1", AGENT_PORTAL_IMAGE_ID)));
		appServerASGPropsBuilder.keyName(OctankAgentPortal.KEY_PAIR_NAME);

		appServerASGPropsBuilder.associatePublicIpAddress(false);
		appServerASGPropsBuilder.minCapacity(2).maxCapacity(4);

		Role agentPortalEC2Role = IAMUtils.createEC2Role(this, "AgentPortalAppRole", "Octank_AgentPortal_EC2",
			"SecretsManagerReadWrite", "AmazonS3FullAccess", "CloudWatchAgentServerPolicy",
			"AmazonSSMManagedInstanceCore", "AmazonRDSDataFullAccess");
		appServerASGPropsBuilder.role(agentPortalEC2Role);

		appServerASGPropsBuilder.groupMetrics(Collections.singletonList(GroupMetrics.all()));

		appServerASGPropsBuilder.healthCheck(software.amazon.awscdk.services.autoscaling.HealthCheck.elb(
			ElbHealthCheckOptions.builder().grace(Duration.minutes(5)).build()));

		Vpc vpc = networkStack.getVpc();
		appServerASGPropsBuilder.vpc(vpc);

		List<ISubnet> appSubnets = new ArrayList<>();

		vpc.getPrivateSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.APP_SUBNET_NAME)) {
				appSubnets.add(iSubnet);
			}
		});

		appServerASGPropsBuilder.vpcSubnets(SubnetSelection.builder().subnets(appSubnets).build());

		Map<String, SecurityGroup> securityGroups = networkStack.getSecurityGroups();
		appServerASGPropsBuilder.securityGroup(securityGroups.get(NetworkStack.APP_SG_ID));

		return new AutoScalingGroup(this, "AppServerAsg", appServerASGPropsBuilder.build());
	}

	private final AutoScalingGroup _appServerASG;

	private ApplicationLoadBalancer _appServerALB;

}
