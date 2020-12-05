import boto3
import json
import logging
import time

from random import seed
from random import randint

seed(1)

transcribe = boto3.client('transcribe')
comprehend = boto3.client('comprehend')
dynamodb = boto3.resource('dynamodb')
s3 = boto3.resource('s3')

logger = logging.getLogger(__name__)


def cut_str_to_bytes(s, max_bytes):
    # cut it twice to avoid encoding potentially GBs of `s` just to get e.g. 10 bytes?
    b = s[:max_bytes].encode('utf-8')[:max_bytes]

    if b[-1] & 0b10000000:
        last_11xxxxxx_index = [i for i in range(-1, -5, -1)
                               if b[i] & 0b11000000 == 0b11000000][0]
        # note that last_11xxxxxx_index is negative

        last_11xxxxxx = b[last_11xxxxxx_index]
        last_char_length = 0
        if not last_11xxxxxx & 0b00100000:
            last_char_length = 2
        elif not last_11xxxxxx & 0b0010000:
            last_char_length = 3
        elif not last_11xxxxxx & 0b0001000:
            last_char_length = 4

        if last_char_length > -last_11xxxxxx_index:
            # remove the incomplete character
            b = b[:last_11xxxxxx_index]

    return b.decode('utf-8')


def get_bucket_key(url):
    s3_uri = url[url.index('.com') + 4:]

    first_index = s3_uri.index('/') + 1
    second_index = s3_uri.index('/', first_index)
    bucket = s3_uri[first_index:second_index]
    key = s3_uri[second_index + 1:]

    return [bucket, key]


def lambda_handler(event, context):
    transcription_job_name = event['detail']['TranscriptionJobName']

    try:
        transcription_job_response = transcribe.get_transcription_job(TranscriptionJobName=transcription_job_name)

        file_url = transcription_job_response['TranscriptionJob']['Transcript']['RedactedTranscriptFileUri']

        bucket_key = get_bucket_key(file_url)

        bucket = s3.Bucket(bucket_key[0])

        transcription_result = json.loads(bucket.Object(bucket_key[1]).get()['Body'].read())

        transcript = transcription_result['results']['transcripts'][0]['transcript']

        transcript = cut_str_to_bytes(transcript, 5000)

        key_phrases_response = comprehend.detect_key_phrases(Text=transcript, LanguageCode='en')

        phrases_list = []
        for key_phrase in key_phrases_response['KeyPhrases']:
            phrase_text = key_phrase['Text']
            logger.info('key_phrases: ' + phrase_text)
            if 'PII' not in phrase_text:
                phrases_list.append({'S': phrase_text})

        sentiment_response = comprehend.detect_sentiment(Text=transcript, LanguageCode='en')
        logger.info('sentiment: ' + sentiment_response['Sentiment'])

        agent_call_item = {
            'producerId': str(randint(100000, 500000)),
            'callTimeStamp': int(round(time.time() * 1000)),
            'sentiment': sentiment_response['Sentiment'],
            'keyPhrases': phrases_list
        }

        table = dynamodb.Table('AgentCalls')

        table.put_item(Item=agent_call_item)

        return "Ok"

    except Exception as e:
        logger.exception(e)
        raise e
