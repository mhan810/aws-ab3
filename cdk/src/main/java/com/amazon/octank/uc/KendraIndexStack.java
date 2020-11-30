package com.amazon.octank.uc;

import com.amazon.octank.Environment;
import com.amazon.octank.security.EncryptionKeyStack;
import com.amazon.octank.util.IAMUtils;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.kendra.CfnIndex;
import software.amazon.awscdk.services.kendra.CfnIndexProps;

import java.util.Objects;

/**
 * @author Michael C. Han (mhnmz)
 */
public class KendraIndexStack extends Stack {

	public KendraIndexStack(
		final software.constructs.Construct scope, final String id, final StackProps props,
		final EncryptionKeyStack encryptionKeyStack, final Environment environment) {
		super(scope, id, props);

		CfnIndexProps.Builder indexPropsBuilder = CfnIndexProps.builder().name("OctankQAIndex");
		indexPropsBuilder.description("Index to support Octank Q&A Chatbots");

		if (Objects.equals(environment, Environment.NON_PRODUCTION)) {
			indexPropsBuilder.edition("DEVELOPER_EDITION");
		}
		else {
			indexPropsBuilder.edition("ENTERPRISE_EDITION");
		}

		String dataEncryptionKeyId = encryptionKeyStack.getDataEncryptionKey().getKeyId();

		indexPropsBuilder.serverSideEncryptionConfiguration(
			CfnIndex.ServerSideEncryptionConfigurationProperty.builder().kmsKeyId(dataEncryptionKeyId).build());

		Role kendraRole = IAMUtils.createServiceRole(
			this, "OctankKendraRole", new ServicePrincipal("kendra.amazonaws.com"), "OctankKendraRole");

		IAMUtils.addInlinePolicy(kendraRole, "S3Kms", _KENDRA_POLICY_FILE);

		IAMUtils.addKmsPolicy(kendraRole);

		indexPropsBuilder.roleArn(kendraRole.getRoleArn());

		_kendraIndex = new CfnIndex(this, "OctankKendraIndex", indexPropsBuilder.build());
	}

	public CfnIndex getKendraIndex() {
		return _kendraIndex;
	}

	private static final String _KENDRA_POLICY_FILE = "/META-INF/iampolicies/kendra.json";
	private final CfnIndex _kendraIndex;

}

