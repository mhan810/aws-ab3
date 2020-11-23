package com.amazon.octank.security;

import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PrincipalBase;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael C. Han (mhnmz)
 */
public class IAMStack extends Stack {

	public IAMStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		RoleProps.Builder agentPortalAppRoleBuilder = RoleProps.builder().roleName("Octank_AgentPortal_EC2");

		List<IManagedPolicy> managedPolicies = new ArrayList<>();

		managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName("SecretsManagerReadWrite"));
		managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName("AmazonS3FullAccess"));
		managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"));
		managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"));
		managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName("AmazonRDSDataFullAccess"));

		agentPortalAppRoleBuilder.managedPolicies(managedPolicies);
		agentPortalAppRoleBuilder.assumedBy(new ServicePrincipal("ec2.amazonaws.com"));

		_agentPortalEC2Role = new Role(this, "AgentPortalAppRole", agentPortalAppRoleBuilder.build());
	}

	public Role getAgentPortalEC2Role() {
		return _agentPortalEC2Role;
	}

	private final Role _agentPortalEC2Role;

}
