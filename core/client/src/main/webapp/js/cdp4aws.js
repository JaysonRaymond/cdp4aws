/**
 * CDP4AWS: Cloud Design Patterns for AWS
 * A library of functions  useful for implementing Cloud Design Patterns using Amazon Web Services.
 * Assumes the AWS SDK JavaScript Library has been loaded.
 */
var CDP4AWS = (function (AWS) {

    // used to expose public static methods
    var staticMethods = {
    };
    var instance = {
        awsAccountNumber: "ERROR: CDP4AWS.awsAccountNumber has not been set!"
    };

    var sqs = new AWS.SQS();

    var resultsQueueName = generateUniqueResultQueueName();
    var requestsQueue;
    var resultsQueue;
    var requests = {};
    var messagesSent = 0;
    var messagesReceived = 0;
    var backOff = 1;

    function getQueueUrlPrefix() {
        return "https://sqs." + AWS.config.region + ".amazonaws.com/" + instance.awsAccountNumber+"/";
    }

    function getRequestsQueueUrl() {
        return getQueueUrlPrefix()+"Requests";
    }

    function getResultsQueueUrl() {
        return getQueueUrlPrefix()+resultsQueueName;
    }

    /**
     * Create a random queue name with enough bits as to be _extremely_ unlikely for collisions
     * A random number of 2^256 is generally accepted as suitably unlikely for a collision (e.g. Type4 UUID/GUID).
     * Given JavaScripts largest _integer_ is 2^53 - doing this 5 times will give use 2^265 (an extra 2^10). We represent
     * the result as a base 64 number encoded in the set of valid queue name characters. This will not exceed the
     * SQS Queue name character limit of 80 characters.
     *
     */
    function generateUniqueResultQueueName() {
        var queueName = "Results_"; // 8
        var validChars = "0123456789abcdefghijklmnopqrstuvwqyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-";
        var radix = 64; // validChars.length;
        for (var i = 0; i < 5; i++) {
            var remainder = Math.floor(Math.random() * Math.pow(2, 53));
            for (var exponent = 8; exponent > 0; exponent--) {
                var base64 = Math.pow(radix, exponent);
                var digit = Math.floor(remainder / base64);
                queueName = queueName + validChars.substr(digit, 1);
                if (remainder > base64)
                    remainder = remainder - (base64 * digit);
            }
            queueName = queueName + validChars.substr(remainder, 1);
        }
        return queueName;
    }


    /**
     * Remove messages that have been handled from the Results queue.
     */
    function removeHandledMsgsFromQueue(msgsToRemove) {
        if (msgsToRemove.length > 0) {
            var entries = [];
            for (var i = 0; i < msgsToRemove.length; i++) {
                entries[i] = { Id: msgsToRemove[i].MessageId, ReceiptHandle: msgsToRemove[i].ReceiptHandle }
            }
            var params = {
                Entries: entries,
                QueueUrl: getResultsQueueUrl()
            };
            sqs.deleteMessageBatch(params, function (err, data) {
                if (err) console.log(err, err.stack); // an error occurred
                else     console.log(data);           // successful response
            });
        }
    }

    /**
     * Receive our results. The backend service will create a temporary queue to hold the results of our requests.
     *
     * As a best practice, SQS should be setup for Long Polling as it is the most efficient polling implementation,
     * requiring the least number of http request/responses. This translates into not only reduced network usage by
     * client and server, but also fewer header parsings which is particularly costly to mobile device battery usage
     * and responsiveness.
     * See: http://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-long-polling.html
     * @receiveErrorCallback = function(err) called with error encountered during reception
     * @receiveStartCallback = function() called when an attempt to receive occurs
     * @receiveDoneCallback = function() called when no more messages are expected
     */
    function receive(receiveErrorCallback, receiveStartCallback, receiveDoneCallback) {

        if (resultsQueue === undefined)
        // We'll set the Queue to hold the connection for 1 second, and reconnect every 2.
            resultsQueue = new AWS.SQS({params: {QueueUrl: getResultsQueueUrl(), Attributes: {  ReceiveMessageWaitTimeSeconds: "1" }  }});

        var params = {
            QueueUrl: getResultsQueueUrl(),
            MaxNumberOfMessages: 10, // how many messages do we want to retrieve?
            VisibilityTimeout: 60, // seconds - how long we want a lock on this job
            WaitTimeSeconds: 15, // seconds - how long should we wait for a message?
            MessageAttributeNames: [
                'CorrelationId'
            ]
        }

        receiveStartCallback();

        resultsQueue.receiveMessage(params, function (err, data) {
            if (err) {
                if (err.toString().indexOf("AWS.SimpleQueueService.NonExistentQueue: The specified queue does not exist") > 0) {
                    backOff += backOff;
                    console.info("Results Queue not found yet, retrying again in " + backOff + " seconds.\r\n");
                    setTimeout(function () {
                        receive(receiveErrorCallback, receiveStartCallback, receiveDoneCallback)
                    }, backOff * 1000);
                    return;
                } else {
                    receiveErrorCallback(err);
                }
            }
            else if (data) {
                messagesReceived += data.Messages.length;
                for (var i = 0; i < data.Messages.length; i++) {
                    var correlationId = data.Messages[i].MessageAttributes['CorrelationId'].StringValue;
                    requests[correlationId](data.Messages[i]);
                }
                removeHandledMsgsFromQueue(data.Messages);
                backOff = 1;
            }
            if (messagesReceived < messagesSent) {
                // more to get..
                receive(receiveErrorCallback, receiveStartCallback, receiveDoneCallback);
            } else {
                // all outstanding messages accounted for.
                receiveDoneCallback();
            }
        });
    }

    /**
     * Queue a request for our command to be executed, notifying the sendCallback with the status of the send,
     * then later when the the results arrive, send them to the receiveCallback function.
     * @request = the String request to be sent
     * @sendCallback = function(request, sendStatus) where 'request' will be your original request and sendStatus is the
     * status returned after accepting the request
     * @receiveCallback = function(result) where 'result' is the result of your request
     * @receiveErrorCallback = function(error) where 'error' is the error received while attempting to receive
     * @receiveStartCallback = function() called when an attempt to receive occurs
     * @receiveDoneCallback = function() called when no further messages are expected
     */
    instance.queueRequest = function (request, sendCallback, sendErrorCallback, receiveCallback, receiveErrorCallback, receiveStartCallback, receiveDoneCallback) {
        if (requestsQueue === undefined)
            requestsQueue = new AWS.SQS({params: {QueueUrl: getRequestsQueueUrl()}});

        requestsQueue.sendMessage(
            {   MessageBody: request,
                MessageAttributes: {
                    ReplyTo: {
                        DataType: 'String',
                        StringValue: resultsQueueName
                    }
                }
            },
            function (err, data) {
                if (err) {
                    sendErrorCallback(err);
                }
                else {
                    messagesSent++;
                    requests[data.MessageId] = receiveCallback;
                    sendCallback(request, data);
                    receive(receiveErrorCallback, receiveStartCallback, receiveDoneCallback);
                }
            }
        );
    }

    /**
     *  Delete the Result Queue when we leave and it's no longer needed.
     *
     *  Because this can fail and we can't always assure clients will do this before disconnecting, a queue reaping
     *  function should be added to the compute node app to garbage collect queues over a certain age since last use.
     *
     *  Some applications may require longer lived queues and will need other means of either managing queue
     *  lifecycle, or use a shared queue and filter messages for a specific user.
     */
    instance.deleteResultQueueOnExit = function () {
        window.onunload = new function () {
            resultsQueue.deleteQueue({ QueueUrl: getResultsQueueUrl() }, function (err, data) {
                if (err) console.log(err, err.stack); // an error occurred
                else     console.log(data);           // successful response
            })
        };
    };

    /**
     * Create a cpd4aws instance.
     * @param awsAccountNumber
     * @param awsAccessKeyId
     * @param awsSecretKey
     * @param awsRegion
     * @returns {*}
     */
    staticMethods.create = function (awsRegion, awsAccountNumber, awsAccessKeyId, awsSecretKey) {
        AWS.config.region = awsRegion;
        instance.awsAccountNumber = awsAccountNumber;
        AWS.config.credentials = { accessKeyId: awsAccessKeyId, secretAccessKey: awsSecretKey };
        return instance;
    }

    return staticMethods;

}(AWS));
