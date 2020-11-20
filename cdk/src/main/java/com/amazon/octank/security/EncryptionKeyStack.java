package com.amazon.octank.security;

import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.kms.KeyProps;
import software.constructs.Construct;

/**
 * @author Michael C. Han (mhnmz)
 */
public class EncryptionKeyStack extends Stack {

	public static final String OCTANK_PASS_KEY_ALIAS = "OctankPassKey";
	public static final String OCTANK_DATA_KEY_ALIAS = "OctankDataKey";

	public EncryptionKeyStack(final Construct scope, final String id, final StackProps stackProps) {
		super(scope, id, stackProps);

		KeyProps dataEncryptionKeyProps = KeyProps.builder().alias(OCTANK_DATA_KEY_ALIAS).description(
			"Key used for encrypting all Octank data").removalPolicy(RemovalPolicy.DESTROY).build();

		_dataEncryptionKey = new Key(this, id + "DataEncryptionKey", dataEncryptionKeyProps);

		KeyProps passEncryptionKeyProps = KeyProps.builder().alias(OCTANK_PASS_KEY_ALIAS).description(
			"Key used for encrypting all Octank data").removalPolicy(RemovalPolicy.DESTROY).build();

		_passEncryptionKey = new Key(this, id + "PassEncryptionKey", passEncryptionKeyProps);
	}

	public Key getDataEncryptionKey() {
		return _dataEncryptionKey;
	}

	public Key getPassEncryptionKey() {
		return _passEncryptionKey;
	}

	private final Key _dataEncryptionKey;
	private final Key _passEncryptionKey;

}
