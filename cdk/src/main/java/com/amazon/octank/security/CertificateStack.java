package com.amazon.octank.security;

import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.acmpca.CfnCertificate;
import software.amazon.awscdk.services.acmpca.CfnCertificateAuthority;
import software.amazon.awscdk.services.acmpca.CfnCertificateAuthorityActivation;
import software.amazon.awscdk.services.acmpca.CfnCertificateAuthorityActivationProps;
import software.amazon.awscdk.services.acmpca.CfnCertificateAuthorityProps;
import software.amazon.awscdk.services.acmpca.CfnCertificateProps;
import software.constructs.Construct;

/**
 * @author Michael C. Han (mhnmz)
 */
public class CertificateStack extends Stack {

	public CertificateStack(final Construct scope, final String id, final StackProps props) {
		super(scope, id, props);

		//create private CA
		CfnCertificateAuthorityProps.Builder cfnCAPropsBuilder = CfnCertificateAuthorityProps.builder();
		cfnCAPropsBuilder.type("ROOT");
		cfnCAPropsBuilder.keyAlgorithm("RSA_4096");
		cfnCAPropsBuilder.signingAlgorithm("SHA512WITHRSA");

		CfnCertificateAuthority.SubjectProperty subjectProperty =
			CfnCertificateAuthority.SubjectProperty.builder().commonName("octank-ab3.com").organization("Octank AB3")
				.country("US").state("IL").build();

		cfnCAPropsBuilder.subject(subjectProperty);

		_cfnCA = new CfnCertificateAuthority(this, "OctankPrivateCA", cfnCAPropsBuilder.build());

		//generate cert w/ private CA
		CfnCertificateProps.Builder caCfnCertPropsBuilder = CfnCertificateProps.builder();

		caCfnCertPropsBuilder.certificateAuthorityArn(_cfnCA.getAttrArn());
		caCfnCertPropsBuilder.certificateSigningRequest(_cfnCA.getAttrCertificateSigningRequest());
		caCfnCertPropsBuilder.signingAlgorithm("SHA512WITHRSA");
		caCfnCertPropsBuilder.validity(CfnCertificate.ValidityProperty.builder().type("YEARS").value(10).build());

		CfnCertificate caCfnCert = new CfnCertificate(this, "OctankPrivateCACert", caCfnCertPropsBuilder.build());

		//activate private CA
		CfnCertificateAuthorityActivationProps.Builder cfnCAActivationPropsBuilder =
			CfnCertificateAuthorityActivationProps.builder();

		cfnCAActivationPropsBuilder.certificateAuthorityArn(_cfnCA.getAttrArn());
		cfnCAActivationPropsBuilder.certificate(caCfnCert.getAttrCertificate());

		CfnCertificateAuthorityActivation cfnCertificateAuthorityActivation = new CfnCertificateAuthorityActivation(
			this, "OctankPrivateCAActivate", cfnCAActivationPropsBuilder.build());

		//create cert for octank-ab3.com

		/*
		@todo CDK certificates do not properly support certs from private CAs

		Certificate certificate = new Certificate(
			this, "Octank Cert", CertificateProps.builder().domainName("*.octank-ab3.com").build()); */

	}

	public CfnCertificateAuthority getCfnCA() {
		return _cfnCA;
	}

	private final CfnCertificateAuthority _cfnCA;

}
