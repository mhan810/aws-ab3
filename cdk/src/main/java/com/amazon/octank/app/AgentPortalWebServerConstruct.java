package com.amazon.octank.app;

import com.amazon.octank.OctankAgentPortal;
import com.amazon.octank.network.NetworkStack;
import com.amazon.octank.util.IAMUtils;
import com.amazon.octank.util.UserDataUtil;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroupProps;
import software.amazon.awscdk.services.autoscaling.ElbHealthCheckOptions;
import software.amazon.awscdk.services.autoscaling.GroupMetrics;
import software.amazon.awscdk.services.ec2.AmazonLinuxCpuType;
import software.amazon.awscdk.services.ec2.AmazonLinuxGeneration;
import software.amazon.awscdk.services.ec2.AmazonLinuxImageProps;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.ListenerCertificate;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroupProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.SslPolicy;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.iam.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class AgentPortalWebServerConstruct extends Construct {

	public AgentPortalWebServerConstruct(
		final software.constructs.Construct scope, final String id, final NetworkStack networkStack,
		final AgentPortalAppServerConstruct agentPortalAppServerConstruct) {

		super(scope, id);

		_webServerASG = addWebServerASG(networkStack, agentPortalAppServerConstruct);
		_webServerALB = addWebServerNLB(networkStack);
	}

	public AutoScalingGroup getWebServerASG() {
		return _webServerASG;
	}

	public NetworkLoadBalancer getWebServerNLB() {
		return _webServerALB;
	}

	private AutoScalingGroup addWebServerASG(
		final NetworkStack networkStack, final AgentPortalAppServerConstruct agentPortalAppServerConstruct) {

		AutoScalingGroupProps.Builder webServerASGPropsBuilder = AutoScalingGroupProps.builder().autoScalingGroupName(
			"WebServerASG");

		webServerASGPropsBuilder.instanceType(InstanceType.of(InstanceClass.BURSTABLE4_GRAVITON, InstanceSize.LARGE));
		webServerASGPropsBuilder.machineImage(MachineImage.latestAmazonLinux(AmazonLinuxImageProps.builder().cpuType(
			AmazonLinuxCpuType.ARM_64).generation(AmazonLinuxGeneration.AMAZON_LINUX_2).build()));
		webServerASGPropsBuilder.keyName(OctankAgentPortal.KEY_PAIR_NAME);

		webServerASGPropsBuilder.associatePublicIpAddress(false);
		webServerASGPropsBuilder.minCapacity(2).maxCapacity(4);

		Role webEC2Role = IAMUtils.createEC2Role(this, "AgentPortalWebRole", "Octank_AgentPortal_Web_EC2",
			"AmazonSSMManagedInstanceCore", "CloudWatchAgentServerPolicy");

		webServerASGPropsBuilder.role(webEC2Role);

		webServerASGPropsBuilder.groupMetrics(Collections.singletonList(GroupMetrics.all()));

		webServerASGPropsBuilder.healthCheck(software.amazon.awscdk.services.autoscaling.HealthCheck.elb(
			ElbHealthCheckOptions.builder().grace(Duration.minutes(5)).build()));

		Vpc vpc = networkStack.getVpc();
		webServerASGPropsBuilder.vpc(vpc);

		List<ISubnet> dmzSubnets = new ArrayList<>();

		vpc.getPublicSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.DMZ_SUBNET_NAME)) {
				dmzSubnets.add(iSubnet);
			}
		});

		webServerASGPropsBuilder.vpcSubnets(SubnetSelection.builder().subnets(dmzSubnets).build());

		Map<String, SecurityGroup> securityGroups = networkStack.getSecurityGroups();
		webServerASGPropsBuilder.securityGroup(securityGroups.get(NetworkStack.DMZ_SG_ID));

		Map<String, String> tokens = new HashMap<>();
		tokens.put("%ALB_URL%", agentPortalAppServerConstruct.getAppServerALB().getLoadBalancerDnsName());

		webServerASGPropsBuilder.userData(UserDataUtil.createUserData(_WEB_SERVER_USER_DATA, tokens));

		return new AutoScalingGroup(this, "WebServerAsg", webServerASGPropsBuilder.build());
	}

	private NetworkLoadBalancer addWebServerNLB(final NetworkStack networkStack) {
		NetworkLoadBalancerProps.Builder nlbPropsBuilder = NetworkLoadBalancerProps.builder().loadBalancerName(
			"WebServerNLB");

		nlbPropsBuilder.internetFacing(true).crossZoneEnabled(true);

		Vpc vpc = networkStack.getVpc();

		nlbPropsBuilder.vpc(vpc);

		List<ISubnet> dmzSubnets = new ArrayList<>();

		vpc.getPublicSubnets().forEach(iSubnet -> {
			if (iSubnet.getNode().getId().contains(NetworkStack.DMZ_SUBNET_NAME)) {
				dmzSubnets.add(iSubnet);
			}
		});

		nlbPropsBuilder.vpcSubnets(SubnetSelection.builder().subnets(dmzSubnets).build());

		_webServerALB = new NetworkLoadBalancer(this, "WebServerNLB", nlbPropsBuilder.build());

		//Create NetworkTargetGroup
		NetworkTargetGroupProps.Builder webServerNTGPropsBuilder = NetworkTargetGroupProps.builder().targetGroupName(
			"WebServerNTG").targetType(TargetType.INSTANCE);

		webServerNTGPropsBuilder.healthCheck(HealthCheck.builder().protocol(Protocol.HTTPS).build());
		webServerNTGPropsBuilder.protocol(Protocol.TLS).port(443);
		webServerNTGPropsBuilder.vpc(vpc);

		NetworkTargetGroup networkTargetGroup = new NetworkTargetGroup(
			this, "WebServerATG", webServerNTGPropsBuilder.build());

		_webServerASG.attachToNetworkTargetGroup(networkTargetGroup);

		List<NetworkTargetGroup> networkTargetGroups = Collections.singletonList(networkTargetGroup);

		//Create NLB Listener
		BaseNetworkListenerProps.Builder sslNLBListenerProps = BaseNetworkListenerProps.builder();
		sslNLBListenerProps.protocol(Protocol.TLS).port(443);
		sslNLBListenerProps.sslPolicy(SslPolicy.RECOMMENDED);

		sslNLBListenerProps.defaultTargetGroups(networkTargetGroups);

		ListenerCertificate listenerCertificate = ListenerCertificate.fromArn(_NLB_SSL_CERT);

		sslNLBListenerProps.certificates(Collections.singletonList(listenerCertificate));

		_webServerALB.addListener("AgentPortalALBHttp", sslNLBListenerProps.build());

		return _webServerALB;
	}

	private static final String _NLB_SSL_CERT =
		"arn:aws:acm:us-east-1:817387504538:certificate/b4ae6329-de7c-4318-b18e-58b5b59bf786";
	private static final String _WEB_SERVER_USER_DATA = "/META-INF/userdata/webserver.sh";
	private final AutoScalingGroup _webServerASG;

	private NetworkLoadBalancer _webServerALB;

}
