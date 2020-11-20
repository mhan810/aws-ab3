package com.amazon.octank.security;

import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.kms.Alias;
import software.amazon.awscdk.services.kms.AliasProps;
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

		KeyProps dataEncryptionKeyProps = KeyProps.builder().alias("octank-data").description(
			"Key used for encrypting all Octank data").build();

		_dataEncryptionKey = new Key(this, id + "DataEncryptionKey", dataEncryptionKeyProps);

		_dataEncryptionKeyAlias = new Alias(this, id + "DataEncryptionKeyAlias", AliasProps.builder().aliasName(
			OCTANK_DATA_KEY_ALIAS).targetKey(_dataEncryptionKey).removalPolicy(RemovalPolicy.DESTROY).build());

		KeyProps passEncryptionKeyProps = KeyProps.builder().alias("octank-pass-key").description(
			"Key used for encrypting all Octank data").removalPolicy(RemovalPolicy.DESTROY).build();

		_passEncryptionKey = new Key(this, id + "PassEncryptionKey", passEncryptionKeyProps);

		_passEncryptionKeyAlias = new Alias(this, id + "PassEncryptionKeyAlias", AliasProps.builder().aliasName(
			OCTANK_PASS_KEY_ALIAS).targetKey(_dataEncryptionKey).removalPolicy(RemovalPolicy.DESTROY).build());
	}

	public Key getDataEncryptionKey() {
		return _dataEncryptionKey;
	}

	public Key getPassEncryptionKey() {
		return _passEncryptionKey;
	}

	public Alias getDataEncryptionKeyAlias() {
		return _dataEncryptionKeyAlias;
	}

	public Alias getPassEncryptionKeyAlias() {
		return _passEncryptionKeyAlias;
	}

	private final Key _dataEncryptionKey;
	private final Key _passEncryptionKey;
	private final Alias _dataEncryptionKeyAlias;
	private final Alias _passEncryptionKeyAlias;
}
