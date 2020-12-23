import os
import json
import boto3
from datetime import datetime

# Import resources using AWS Python SDK (boto3) and specify the DynamoDB table to scan and S3 bucket to write file to
# Table and bucket name are passed as environment variables in SAM template
s3 = boto3.resource('s3')
bucket = s3.Bucket(os.environ['BUCKET_NAME'])
table = boto3.resource('dynamodb').Table(os.environ['TABLE_NAME'])


def lambda_handler(event, context):
    # Scan DynamoDB contents and save to array
    # In my case, my DynamoDB table contains data on my app's subscribers
    all_subscribers_data = scan_subscribers_table()

    todays_date = format_date(datetime.today())

    # Add ReportingDate to each row of data for filtering in QuickSight
    data_rows = convert_to_rows(all_subscribers_data, todays_date)

    # Save JSON data file to S3
    response = write_to_s3(data_rows, todays_date)


def scan_subscribers_table():
    # Loop through subscribers in DynamoDB
    results = table.scan(
        Select="ALL_ATTRIBUTES"
    )

    all_subscribers = results['Items']

    return all_subscribers


def convert_to_rows(all_subscribers_data, todays_date):
    data_rows = []

    # Append today's date to each item as ReportingDate
    for item in all_subscribers_data:
        item['ReportingDate'] = todays_date
        data_rows.append(item)

    return data_rows


# Save JSON data file to S3
# The name of the file is set to the current date
def write_to_s3(data_rows, todays_date):
    response = bucket.put_object(
        Body=json.dumps(data_rows).encode('UTF-8'),
        Key=f'{todays_date}.json'
    )

    return response


# Return a date with format YYYY-MM-DD
def format_date(date_object):
    formatted_date = date_object.strftime('%Y-%m-%d')

    return formatted_date