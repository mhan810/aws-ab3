import boto3
import logging
import urllib.parse

kendra = boto3.client('kendra')

logger = logging.getLogger(__name__)


def lambda_handler(event, context):
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        faq_name = 'faq_' + key[0:key.rindex('.csv')].replace('/', '_')

        list_indices_response = kendra.list_indices(MaxResults=10)
        index_id = list_indices_response['IndexConfigurationSummaryItems'][0]['Id']

        faq_args = {
            'IndexId': index_id,
            'Name': faq_name,
            'Description': 'Octank FAQ',
            'S3Path': {
                'Bucket': bucket,
                'Key': key
            },
            'RoleArn': 'arn:aws:iam::817387504538:role/Octank_Lambda_Function',
            'Tags': [{
                'Key': 'project',
                'Value': 'AB3'
            }],
            'FileFormat': 'CSV'
        }

        response = kendra.create_faq(**faq_args)

        logger.info('Created FAQ' + response['Id'])

        return response['Id']

    except Exception as e:
        logger.exception(e)
        raise e
