package com.amazon.octank.storage;

import com.amazon.octank.security.EncryptionKeyStack;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
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

		BucketProps.Builder mlOutputProps = createBucketPropsBuilder(encryptionKeyStack, "octank-ab3-ml-output", true);

		mlOutputProps.serverAccessLogsBucket(_logsBucket);
		mlOutputProps.serverAccessLogsPrefix("/access-logs/s3");

		_mlOutputBucket = new Bucket(this, "OctankMLBucket", mlOutputProps.build());
	}

	public Bucket getDataBucket() {
		return _dataBucket;
	}

	public Bucket getLogsBucket() {
		return _logsBucket;
	}

	private BucketProps.Builder createBucketPropsBuilder(
		final EncryptionKeyStack encryptionKeyStack, final String bucketName, final boolean versioned) {

		BucketProps.Builder bucketProps = BucketProps.builder().bucketName(bucketName);

		bucketProps.blockPublicAccess(BlockPublicAccess.BLOCK_ALL).publicReadAccess(false);
		//bucketProps.encryption(BucketEncryption.KMS);
		//bucketProps.encryptionKey(encryptionKeyStack.getDataEncryptionKey());
		bucketProps.versioned(versioned);

		return bucketProps;
	}

	private final Bucket _dataBucket;
	private final Bucket _logsBucket;

	private final Bucket _mlOutputBucket;
}