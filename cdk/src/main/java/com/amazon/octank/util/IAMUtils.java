package com.amazon.octank.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awscdk.services.iam.IManagedPolicy;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.constructs.Construct;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Michael C. Han (mhnmz)
 */
public class IAMUtils {

	public static void addInlinePolicy(Role role, String id, String jsonPolicyFileName) {
		try {
			InputStream secretsManagerPolicy = IAMUtils.class.getResourceAsStream(jsonPolicyFileName);

			PolicyDocument policyDocument = PolicyDocument.fromJson(
				new ObjectMapper().readValue(secretsManagerPolicy, HashMap.class));

			Policy secretsManagerKmsPolicy = new Policy(role, id,
				PolicyProps.builder().document(policyDocument).build());

			role.attachInlinePolicy(secretsManagerKmsPolicy);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void addKmsPolicy(Role role) {
		addInlinePolicy(role, "KmsPolicy", _KMS_POLICY_FILE);
	}

	public static Role createEC2Role(
		final Construct scope, final String id, final String roleName, final String... awsManagedPolicyNames) {

		return createServiceRole(scope, id, new ServicePrincipal("ec2.amazonaws.com"), roleName,
			awsManagedPolicyNames);
	}

	public static Role createServiceRole(
		final Construct scope, String id, final ServicePrincipal servicePrincipal, String roleName,
		String... awsManagedPolicyNames) {

		RoleProps.Builder rolePropsBuilder = RoleProps.builder().roleName(roleName);

		List<IManagedPolicy> managedPolicies = new ArrayList<>(awsManagedPolicyNames.length);

		Arrays.stream(awsManagedPolicyNames).forEach(
			awsManagedPolicyName -> managedPolicies.add(ManagedPolicy.fromAwsManagedPolicyName(awsManagedPolicyName)));

		rolePropsBuilder.managedPolicies(managedPolicies);
		rolePropsBuilder.assumedBy(servicePrincipal);

		return new Role(scope, id, rolePropsBuilder.build());
	}

	private static final String _KMS_POLICY_FILE = "/META-INF/iampolicies/kms.json";

}
