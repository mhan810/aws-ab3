import boto3
import logging
import time
import urllib.parse

transcribe = boto3.client('transcribe')

logger = logging.getLogger(__name__)

def lambda_handler(event, context):
    # Get the object from the event and show its content type
    bucket = event['Records'][0]['s3']['bucket']['name']
    key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'], encoding='utf-8')
    try:
        job_name = 'transcribe' + key[0:key.rindex('.wav')].replace('/', '_') + str(int(round(time.time() * 1000)))
        media_uri = f's3://{bucket}/{key}'
        job_args = {
            'TranscriptionJobName': job_name,
            'Media': {'MediaFileUri': media_uri},
            'MediaFormat': 'wav',
            'LanguageCode': 'en-US',
            'OutputBucketName': 'octank-ab3-ml-output',
            'OutputKey': 'connect/transcription/' + key[0:key.rindex('.wav')] + '.json',
            'ContentRedaction': {
                'RedactionType': 'PII',
                'RedactionOutput': 'redacted'},
            'Settings': {
                'ShowSpeakerLabels': True,
                'MaxSpeakerLabels': 5}
        }

        response = transcribe.start_transcription_job(**job_args)

        logger.info('registered job' + response['TranscriptionJob']['TranscriptionJobName'])

        return response['TranscriptionJob']['TranscriptionJobName']

    except Exception as e:
        logger.exception(e)
        raise e
