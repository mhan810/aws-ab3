import time
import os
import logging
import boto3
import json

client = boto3.client('connect')

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)

contact_flow_id = 'f6e8e92f-d02a-42b9-9b32-38ef61ea35c4'
connect_instance_id = '4da4153d-abd3-479a-b58f-15759af663b8'
source_phone_number = '+15852830636'
queueId = 'Agents Help Center'

""" --- Helpers to build responses which match the structure of the necessary dialog actions --- """


def get_slots(intent_request):
    return intent_request['currentIntent']['slots']


def close(session_attributes, fulfillment_state, message):
    response = {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'Close',
            'fulfillmentState': fulfillment_state,
            'message': message
        }
    }

    return response


def delegate(session_attributes, slots):
    return {
        'sessionAttributes': session_attributes,
        'dialogAction': {
            'type': 'Delegate',
            'slots': slots
        }
    }


""" --- Helper Functions --- """


def connect_outbound_api(customer_name, customer_phone):
    client.start_outbound_voice_contact(
        DestinationPhoneNumber=customer_phone,
        ContactFlowId=contact_flow_id,
        InstanceId=connect_instance_id,
        QueueId=queueId,
        SourcePhoneNumber=source_phone_number,
        Attributes={
            'Customer Name': customer_name
        })


""" --- Functions that control the bot's behavior --- """


def transfer_call_to_agent(intent_request):
    """
    Performs dialog management and fulfillment for transfer call.
    """
    customer_name = get_slots(intent_request)["Name"]
    customer_phone = get_slots(intent_request)["Number"]
    connect_outbound_api(customer_name, customer_phone)

    return close(intent_request['sessionAttributes'],
                 'Fulfilled',
                 {'contentType': 'PlainText',
                  'content': 'Okay, our representative will call you shortly. Thanks.'})


""" --- Intents --- """


def dispatch(intent_request):
    """
    Called when the user specifies an intent for this bot.
    """
    logger.debug(
        'dispatch userId={}, intentName={}'.format(intent_request['userId'], intent_request['currentIntent']['name']))
    intent_name = intent_request['currentIntent']['name']

    # Dispatch to your bot's intent handlers
    if intent_name == 'AgentCall':
        return transfer_call_to_agent(intent_request)

    raise Exception('Intent with name ' + intent_name + ' not supported')


""" --- Main handler --- """


def lambda_handler(event, context):
    """
    Route the incoming request based on intent.
    The JSON body of the request is provided in the event slot.
    """
    logger.debug(json.dumps(event))
    # By default, treat the user request as coming from the America/New_York time zone.
    os.environ['TZ'] = 'America/New_York'
    time.tzset()
    logger.debug('event.bot.name={}'.format(event['bot']['name']))
    return dispatch(event)
