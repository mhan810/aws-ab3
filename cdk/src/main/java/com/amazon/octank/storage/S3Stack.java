package com.amazon.octank.storage;

import com.amazon.octank.security.EncryptionKeyStack;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketEncryption;
import software.amazon.awscdk.services.s3.BucketProps;
import software.constructs.Construct;

/**
 * @author Michael C. Han (mhnmz)
 */
public class S3Stack extends Stack {

	public S3Stack(
		final Construct scope, final String id, final StackProps props, final EncryptionKeyStack encryptionKeyStack) {

		super(scope, id, props);

		BucketProps.Builder logsBucketProps = createBucketPropsBuilder(encryptionKeyStack, "octank-ab3-logs", false);

		_logsBucket = new Bucket(this, "OctankLogsBucket", logsBucketProps.build());

		BucketProps.Builder dataBucketProps = createBucketPropsBuilder(encryptionKeyStack, "octank-ab3-data", true);

		dataBucketProps.serverAccessLogsBucket(_logsBucket);
		dataBucketProps.serverAccessLogsPrefix("/access-logs/s3");

		_dataBucket = new Bucket(this, "OctankDataBucket", dataBucketProps.build());

	}

	public Bucket getDataBucket() {
		return _dataBucket;
	}

	public Bucket getLogsBucket() {
		return _logsBucket;
	}

	private BucketProps.Builder createBucketPropsBuilder(
		final EncryptionKeyStack encryptionKeyStack, final String bucketName, final boolean versioned) {

		BucketProps.Builder logsBucketProps = BucketProps.builder().bucketName(bucketName);

		logsBucketProps.blockPublicAccess(BlockPublicAccess.BLOCK_ALL).publicReadAccess(false);
		logsBucketProps.encryption(BucketEncryption.KMS);
		logsBucketProps.encryptionKey(encryptionKeyStack.getDataEncryptionKey());
		logsBucketProps.versioned(versioned);

		return logsBucketProps;
	}

	private final Bucket _dataBucket;
	private final Bucket _logsBucket;

}