package com.amazon.octank.app;


import com.amazon.octank.OctankAgentPortal;
import com.amazon.octank.network.NetworkStack;
import com.amazon.octank.util.IAMUtils;
import com.amazon.octank.util.UserDataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
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
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.Role;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalAppServerConstruct extends Construct {

	public AgentPortalAppServerConstruct(
		final software.constructs.Construct scope, final String id, final NetworkStack networkStack) {

		super(scope, id);

		_appServerASG = addAppServerASG(networkStack);
		_appServerALB = addAppServerALB(networkStack);
	}

	public ApplicationLoadBalancer getAppServerALB() {
		return _appServerALB;
	}

	public AutoScalingGroup getAppServerASG() {
		return _appServerASG;
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

		//Create ApplicationTargetGroup
		ApplicationTargetGroupProps.Builder appServerATGPropsBuilder =
			ApplicationTargetGroupProps.builder().targetGroupName("AppServerATG").targetType(TargetType.INSTANCE);

		appServerATGPropsBuilder.healthCheck(HealthCheck.builder().protocol(Protocol.HTTPS).build());
		appServerATGPropsBuilder.protocol(ApplicationProtocol.HTTPS).port(443);
		appServerATGPropsBuilder.vpc(vpc);
		appServerATGPropsBuilder.stickinessCookieDuration(Duration.minutes(60));

		ApplicationTargetGroup applicationTargetGroup = new ApplicationTargetGroup(
			this, "AgentPortalATG", appServerATGPropsBuilder.build());

		_appServerASG.attachToApplicationTargetGroup(applicationTargetGroup);

		List<ApplicationTargetGroup> applicationTargetGroups = Collections.singletonList(applicationTargetGroup);

		//Create ALB Listener
		BaseApplicationListenerProps.Builder sslAppListenerPropsBuilder = BaseApplicationListenerProps.builder();
		sslAppListenerPropsBuilder.open(true);
		sslAppListenerPropsBuilder.protocol(ApplicationProtocol.HTTPS).port(443).sslPolicy(SslPolicy.RECOMMENDED);

		sslAppListenerPropsBuilder.defaultTargetGroups(applicationTargetGroups);

		ListenerCertificate listenerCertificate = ListenerCertificate.fromArn(_ALB_SSL_CERT);

		sslAppListenerPropsBuilder.certificates(Collections.singletonList(listenerCertificate));

		_appServerALB.addListener("AgentPortalALBHttp", sslAppListenerPropsBuilder.build());

		return _appServerALB;
	}

	private AutoScalingGroup addAppServerASG(NetworkStack networkStack) {
		AutoScalingGroupProps.Builder appServerASGPropsBuilder = AutoScalingGroupProps.builder().autoScalingGroupName(
			"AgentAppServerASG");

		appServerASGPropsBuilder.instanceType(InstanceType.of(InstanceClass.STANDARD5, InstanceSize.XLARGE));
		appServerASGPropsBuilder.machineImage(
			MachineImage.genericLinux(Collections.singletonMap("us-east-1", _AGENT_PORTAL_IMAGE_ID)));
		appServerASGPropsBuilder.keyName(OctankAgentPortal.KEY_PAIR_NAME);

		appServerASGPropsBuilder.associatePublicIpAddress(false);
		appServerASGPropsBuilder.minCapacity(2).maxCapacity(4);

		Role agentPortalEC2Role = IAMUtils.createEC2Role(this, "AgentPortalAppRole", "Octank_AgentPortal_EC2",
			"AWSKeyManagementServicePowerUser", "AmazonRDSDataFullAccess", "AmazonS3FullAccess",
			"AmazonSSMManagedInstanceCore", "CloudWatchAgentServerPolicy", "SecretsManagerReadWrite");

		try {
			InputStream secretsManagerPolicy = getClass().getResourceAsStream(_SECRETSMANAGER_KMS_POLICY_FILE);

			PolicyDocument policyDocument = PolicyDocument.fromJson(
				new ObjectMapper().readValue(secretsManagerPolicy, HashMap.class));

			Policy secretsManagerKmsPolicy = new Policy(agentPortalEC2Role, "SecretsManagerKmsPolicy",
				PolicyProps.builder().document(policyDocument).build());

			agentPortalEC2Role.attachInlinePolicy(secretsManagerKmsPolicy);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

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

		appServerASGPropsBuilder.userData(UserDataUtil.createUserData(_AGENT_PORTAL_USER_DATA));

		return new AutoScalingGroup(this, "AppServerAsg", appServerASGPropsBuilder.build());
	}

	private static final String _AGENT_PORTAL_IMAGE_ID = "ami-06e67e0f555078269";
	private static final String _AGENT_PORTAL_USER_DATA = "/META-INF/userdata/agent_portal.sh";
	private static final String _ALB_SSL_CERT =
		"arn:aws:acm:us-east-1:817387504538:certificate/b4ae6329-de7c-4318-b18e-58b5b59bf786";
	private static final String _SECRETSMANAGER_KMS_POLICY_FILE = "/META-INF/iampolicies/secretsmanager_kms.json";

	private final AutoScalingGroup _appServerASG;

	private ApplicationLoadBalancer _appServerALB;

}
