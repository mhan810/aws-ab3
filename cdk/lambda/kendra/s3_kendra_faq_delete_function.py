import boto3
import logging
import urllib.parse

kendra = boto3.client('kendra')

logger = logging.getLogger(__name__)


def lambda_handler(event, context):
    # Get the object from the event and show its content type
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')

    try:
        faq_name = 'faq_' + key[0:key.rindex('.csv')].replace('/', '_')

        list_indices_response = kendra.list_indices(MaxResults=10)
        index_id = list_indices_response['IndexConfigurationSummaryItems'][0]['Id']

        list_faqs_response = kendra.list_faqs(IndexId=index_id, MaxResults=10)
        faqs = list_faqs_response['FaqSummaryItems']
        faq_id = None

        for faq in faqs:
            if faq['Name'] == faq_name:
                faq_id = faq['Id']

        if not faq_id:
            return 'Nothing to Delete'

        kendra.delete_faq(Id=faq_id, IndexId=index_id)

        return 'Deleted ' + faq_id

    except Exception as e:
        logger.exception(e)
        raise e
