package com.amazon.octank.kendra;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.kendra.KendraAsyncClient;
import software.amazon.awssdk.services.kendra.KendraClient;
import software.amazon.awssdk.services.kendra.model.CreateFaqRequest;
import software.amazon.awssdk.services.kendra.model.CreateFaqResponse;
import software.amazon.awssdk.services.kendra.model.FaqFileFormat;
import software.amazon.awssdk.services.kendra.model.IndexConfigurationSummary;
import software.amazon.awssdk.services.kendra.model.ListIndicesRequest;
import software.amazon.awssdk.services.kendra.model.ListIndicesResponse;
import software.amazon.awssdk.services.kendra.model.S3Path;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Michael C. Han (mhnmz)
 */
public class KendraFAQLoaderRequestHandler implements RequestHandler<S3Event, String> {

	@Override
	public String handleRequest(final S3Event s3Event, final Context context) {
		KendraClient kendraClient = KendraClient.create();

		ListIndicesResponse listIndicesResponse = kendraClient.listIndices(ListIndicesRequest.builder().build());

		if (listIndicesResponse.indexConfigurationSummaryItems().isEmpty()) {
			return "No Indices";
		}

		IndexConfigurationSummary indexConfigurationSummary =
			listIndicesResponse.indexConfigurationSummaryItems().get(0);

		String indexId = indexConfigurationSummary.id();

		KendraAsyncClient kendraAsyncClient = KendraAsyncClient.create();

		List<S3EventNotification.S3EventNotificationRecord> s3EventRecords = s3Event.getRecords();

		s3EventRecords.forEach(s3EventNotificationRecord -> {
			S3EventNotification.S3Entity s3Entity = s3EventNotificationRecord.getS3();

			S3EventNotification.S3ObjectEntity s3ObjectEntity = s3Entity.getObject();

			String objectKey = s3ObjectEntity.getUrlDecodedKey();

			if (!objectKey.contains(_KENDRA_FAQ_PREFIX)) {
				return;
			}

			CreateFaqRequest.Builder createFaqRequestBuilder = CreateFaqRequest.builder();
			createFaqRequestBuilder.name(objectKey).fileFormat(FaqFileFormat.CSV);
			createFaqRequestBuilder.indexId(indexId);
			createFaqRequestBuilder.s3Path(S3Path.builder().bucket(s3Entity.getBucket().getName()).key(objectKey)
				                               .build());
			createFaqRequestBuilder.roleArn(_KENDRA_FAQ_ROLE_ARN);
			createFaqRequestBuilder.description(
				"FAQ for file: s3://" + s3Entity.getBucket().getName() + "/" + s3ObjectEntity.getKey());

			CompletableFuture<CreateFaqResponse> createFaqResponse =
				kendraAsyncClient.createFaq(createFaqRequestBuilder.build());
		});

		return "Ok";
	}

	private static final String _KENDRA_FAQ_PREFIX = "kendra-faq";
	private static final String _KENDRA_FAQ_ROLE_ARN = "arn:aws:iam::817387504538:role/OctankKendraFaqRole";

	private static Logger _logger = LoggerFactory.getLogger(KendraFAQLoaderRequestHandler.class);

}
