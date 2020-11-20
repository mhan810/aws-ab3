package com.amazon.octank.security;

import com.amazon.octank.Environment;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.kms.Key;
import software.amazon.awscdk.services.kms.KeyProps;
import software.constructs.Construct;

import java.util.Objects;

/**
 * @author Michael C. Han (mhnmz)
 */
public class EncryptionKeyStack extends Stack {

	public static final String OCTANK_DATA_KEY_ALIAS = "OctankDataKey";
	public static final String OCTANK_PASS_KEY_ALIAS = "OctankPassKey";

	public EncryptionKeyStack(
		final Construct scope, final String id, final StackProps stackProps, final Environment environment) {
		super(scope, id, stackProps);

		KeyProps.Builder dataEncryptionKeyPropsBuilder = KeyProps.builder().alias(OCTANK_DATA_KEY_ALIAS).description(
			"Key used for encrypting all Octank data");

		KeyProps.Builder passEncryptionKeyPropsBuilder = KeyProps.builder().alias(OCTANK_PASS_KEY_ALIAS).description(
			"Key used for encrypting all Octank data");

		//if in production mode, retail the data encryption key
		if (Objects.equals(environment, Environment.PRODUCTION)) {
			dataEncryptionKeyPropsBuilder.removalPolicy(RemovalPolicy.RETAIN);
		}
		else {
			dataEncryptionKeyPropsBuilder.removalPolicy(RemovalPolicy.RETAIN);
		}
		//always remove the password encryption key
		passEncryptionKeyPropsBuilder.removalPolicy(RemovalPolicy.DESTROY);

		_dataEncryptionKey = new Key(this, id + "DataEncryptionKey", dataEncryptionKeyPropsBuilder.build());
		_passEncryptionKey = new Key(this, id + "PassEncryptionKey", passEncryptionKeyPropsBuilder.build());
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
