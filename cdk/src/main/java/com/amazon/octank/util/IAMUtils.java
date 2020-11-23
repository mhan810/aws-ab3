package com.amazon.octank.util;

import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Michael C. Han (mhnmz)
 */
public class IAMUtils {

	public static Role createEC2Role(final Construct scope, String id, String roleName, String... policyNames) {
		RoleProps.Builder rolePropsBuilder = RoleProps.builder().roleName(roleName);

		List<IManagedPolicy> managedPolicies = new ArrayList<>(policyNames.length);

		Arrays.stream(policyNames).forEach(
			policyName -> managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName(policyName)));

		rolePropsBuilder.managedPolicies(managedPolicies);
		rolePropsBuilder.assumedBy(new ServicePrincipal("ec2.amazonaws.com"));

		return new Role(scope, id, rolePropsBuilder.build());
	}

}
