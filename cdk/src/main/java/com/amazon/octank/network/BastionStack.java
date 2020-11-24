package com.amazon.octank.network;

import com.amazon.octank.OctankAgentPortal;
import com.amazon.octank.util.IAMUtils;
import com.amazon.octank.util.UserDataUtil;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.autoscaling.GroupMetrics;
import software.amazon.awscdk.services.ec2.AmazonLinuxCpuType;
import software.amazon.awscdk.services.ec2.AmazonLinuxGeneration;
import software.amazon.awscdk.services.ec2.AmazonLinuxImageProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class BastionStack extends Stack {

	public BastionStack(
		final Construct scope, final String id, final StackProps props, final NetworkStack networkStack) {

		super(scope, id, props);

		SecurityGroup bastionSecurityGroup = networkStack.getSecurityGroups().get(NetworkStack.BASTION_SG_ID);
		//@todo this isn't best. Should put this a little more securely
		bastionSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(22));

		_bastionASG = addBastionASG(networkStack);
	}

	public AutoScalingGroup getBastionASG() {
		return _bastionASG;
	}

	private AutoScalingGroup addBastionASG(NetworkStack networkStack) {
		AutoScalingGroupProps.Builder bastionASGPropsBuilder = AutoScalingGroupProps.builder().autoScalingGroupName(
			"BastionASG");

		bastionASGPropsBuilder.instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.SMALL));
		bastionASGPropsBuilder.machineImage(MachineImage.latestAmazonLinux(AmazonLinuxImageProps.builder().cpuType(
			AmazonLinuxCpuType.ARM_64).generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build()));
		bastionASGPropsBuilder.keyName(OctankAgentPortal.KEY_PAIR_NAME);

		bastionASGPropsBuilder.associatePublicIpAddress(true);
		bastionASGPropsBuilder.minCapacity(1).maxCapacity(1);

		Role bastionEC2Role = IAMUtils.createEC2Role(this, "BastionAppRole", "Octank_Bastion_EC2",
			"AmazonSSMManagedInstanceCore", "CloudWatchAgentServerPolicy");
		bastionASGPropsBuilder.role(bastionEC2Role);

		bastionASGPropsBuilder.groupMetrics(Collections.singletonList(GroupMetrics.all()));

		bastionASGPropsBuilder.healthCheck(software.amazon.awscdk.services.autoscaling.HealthCheck.ec2());

		Vpc vpc = networkStack.getVpc();
		bastionASGPropsBuilder.vpc(vpc);

		List<ISubnet> dmzSubnets = new ArrayList<>();

		vpc.getPublicSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.DMZ_SUBNET_NAME)) {
				dmzSubnets.add(iSubnet);
			}
		});

		bastionASGPropsBuilder.vpcSubnets(SubnetSelection.builder().subnets(dmzSubnets).build());

		Map<String, SecurityGroup> securityGroups = networkStack.getSecurityGroups();
		bastionASGPropsBuilder.securityGroup(securityGroups.get(NetworkStack.BASTION_SG_ID));

		bastionASGPropsBuilder.userData(UserDataUtil.createUserData(_BASTION_USER_DATA));

		return new AutoScalingGroup(this, "BastionASG", bastionASGPropsBuilder.build());
	}

	private static final String _BASTION_USER_DATA = "/META-INF/userdata/bastion.sh";

	private final AutoScalingGroup _bastionASG;

}