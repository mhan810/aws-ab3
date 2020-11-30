package com.amazon.octank.uc;

import com.amazon.octank.storage.S3Stack;
import com.amazon.octank.util.IAMUtils;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.kendra.CfnFaq;
import software.amazon.awscdk.services.kendra.CfnFaqProps;
import software.constructs.Construct;

/**
 * @author Michael C. Han (mhnmz)
 */
public class KendraFAQStack extends Stack {

	public KendraFAQStack(
		final Construct scope, final String id, final StackProps props, final KendraIndexStack kendraIndexStack,
		final S3Stack s3Stack) {

		super(scope, id, props);

		CfnFaqProps.Builder faqPropsBuilder = CfnFaqProps.builder().name("OctankKendraFAQ");
		faqPropsBuilder.description("FAQ to feed Octank's Chatbot");
		faqPropsBuilder.fileFormat("CSV");
		faqPropsBuilder.indexId(kendraIndexStack.getKendraIndex().getAttrId());

		Role kendraFAQRole = IAMUtils.createServiceRole(this, "OctankKendraFaqRole",
			new ServicePrincipal("kendra.amazonaws.com"), "OctankKendraFaqRole", "AmazonS3FullAccess");

		IAMUtils.addInlinePolicy(kendraFAQRole, "S3Kms", _KMS_POLICY_FILE);

		faqPropsBuilder.roleArn(kendraFAQRole.getRoleArn());

		faqPropsBuilder.s3Path(CfnFaq.S3PathProperty.builder().bucket(s3Stack.getDataBucket().getBucketName())
			                       .key("kendra/help-desk-faq.csv").build());

		CfnFaq cfnFaq = new CfnFaq(this, "OctankKendraFAQ", faqPropsBuilder.build());
	}

	private static final String _KMS_POLICY_FILE = "/META-INF/iampolicies/kendrafaq.json";

}
