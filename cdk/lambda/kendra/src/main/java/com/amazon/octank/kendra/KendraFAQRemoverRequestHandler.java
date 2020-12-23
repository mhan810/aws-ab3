package com.amazon.octank.kendra;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.kendra.KendraAsyncClient;
import software.amazon.awssdk.services.kendra.KendraClient;
import software.amazon.awssdk.services.kendra.model.DeleteFaqRequest;
import software.amazon.awssdk.services.kendra.model.FaqSummary;
import software.amazon.awssdk.services.kendra.model.IndexConfigurationSummary;
import software.amazon.awssdk.services.kendra.model.ListFaqsRequest;
import software.amazon.awssdk.services.kendra.model.ListFaqsResponse;
import software.amazon.awssdk.services.kendra.model.ListIndicesRequest;
import software.amazon.awssdk.services.kendra.model.ListIndicesResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael C. Han (mhnmz)
 */
public class KendraFAQRemoverRequestHandler implements RequestHandler<S3Event, String> {

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

		ListFaqsResponse listFaqsResponse = kendraClient.listFaqs(ListFaqsRequest.builder().indexId(indexId).build());

		Map<String, FaqSummary> faqSummaryMap = new HashMap<>();

		List<FaqSummary> faqSummaries = listFaqsResponse.faqSummaryItems();
		faqSummaries.forEach(faqSummary -> faqSummaryMap.put(faqSummary.name(), faqSummary));

		KendraAsyncClient kendraAsyncClient = KendraAsyncClient.create();

		List<S3EventNotification.S3EventNotificationRecord> s3EventRecords = s3Event.getRecords();

		s3EventRecords.forEach(s3EventNotificationRecord -> {
			S3EventNotification.S3Entity s3Entity = s3EventNotificationRecord.getS3();

			S3EventNotification.S3ObjectEntity s3ObjectEntity = s3Entity.getObject();

			String objectKey = s3ObjectEntity.getUrlDecodedKey();

			if (!objectKey.contains(_KENDRA_FAQ_PREFIX)) {
				return;
			}

			FaqSummary faqSummary = faqSummaryMap.get(objectKey);

			if (faqSummary == null) {
				if (_logger.isInfoEnabled()) {
					_logger.info("No faq for : " + objectKey);
				}

				return;
			}

			DeleteFaqRequest.Builder deleteFaqRequestBuilder = DeleteFaqRequest.builder();
			deleteFaqRequestBuilder.id(faqSummary.id());
			deleteFaqRequestBuilder.indexId(indexId);

			kendraAsyncClient.deleteFaq(deleteFaqRequestBuilder.build());
		});

		return "Ok";
	}

	private static final String _KENDRA_FAQ_PREFIX = "kendra-faq";
	private static Logger _logger = LoggerFactory.getLogger(KendraFAQRemoverRequestHandler.class);

}
