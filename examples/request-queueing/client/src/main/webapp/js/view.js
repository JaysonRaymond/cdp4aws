/**
 * CDP4AWS: Cloud Design Patterns for AWS
 * A library of functions  useful for implementing Cloud Design Patterns using Amazon Web Services.
 * Assumes the AWS SDK JavaScript Library has been loaded.
 */
var VIEW = (function () {

    // used to expose public static methods
    var staticMethods = {
    };
    var instance = {
        awsAccountNumber: "ERROR: CDP4AWS.awsAccountNumber has not been set!"
    };


    /**
      * This section of code is only to display the step through - no
      * actual request queueing happens here - see the next below for that.
      */
     var state = [ {
            description: '',
            messagesImage: 'empty.gif',
            overlayImage: 'empty.gif',
            background: 'RQ-EIP-Background-core.png'
         }, {
             description: 'Client creates a Request containing: a Command, and universally unique Correlation and Session Ids',
             messagesImage: 'RQ-EIP-Creating.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description:'Client sends Command Request to ‘Requests’ Queue',
             messagesImage: 'RQ-EIP-Sending.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'Command Processor polls for Commands on Request Queue',
             messagesImage: 'RQ-EIP-Processing-Receiving.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'Processor transforms (executes) command request to result',
             messagesImage: 'RQ-EIP-Processing-Transforming.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'Creates a Results Queue based on unique session id sent from client and sends result to it',
             messagesImage: 'RQ-EIP-Processing-Sending.gif',
             overlayImage: 'RQ-EIP-MyQueue.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'Meanwhile, client polls for results',
             messagesImage: 'RQ-EIP-Polling.gif',
             overlayImage: 'RQ-EIP-MyQueue.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'Client receives results, matches correlation id of result to original request and notifies app',
             messagesImage: 'RQ-EIP-Receiving.gif',
             overlayImage: 'RQ-EIP-MyQueue.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: 'When demand increases.... ',
             messagesImage: 'RQ-EIP-Others.gif',
             overlayImage: 'RQ-EIP-MyQueue.gif',
             background: 'RQ-EIP-Background-clients.png'
          }, {
             description: '... more processors are automatically added',
             messagesImage: 'RQ-EIP-Others.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background.png'
          }, {
             description: 'As demand wanes - processors are automatically removed',
             messagesImage: 'empty.gif',
             overlayImage: 'RQ-EIP-MyQueue.gif',
             background: 'RQ-EIP-Background-core.png'
          }, {
             description: "Finally the client deletes it's Queue of Results for the session",
             messagesImage: 'empty.gif',
             overlayImage: 'empty.gif',
             background: 'RQ-EIP-Background-core.png'
          } ];

     function get(elementId) {
        return document.getElementById(elementId);
     }

    function toggle(onElementId, offElementId) {
        get(onElementId).style.display = '';
        get(offElementId).style.display = 'none';
    }

     function clearText(textAreaName) {
         get(textAreaName).value = '';
     }

     function showText(textAreaName, text) {
         var textArea = get(textAreaName);
         textArea.value += text;
         textArea.scrollTop = textArea.scrollHeight
     }

     function showImage(layerName, imageName) {
         get(layerName).src = 'img/'+imageName;
     }

     var step = 0;

     instance.nextStep = function() {
         if (++step < state.length) {
             showText('summaryTextArea', '\r\nStep #' + step + ': ' + state[step].description);
             showImage("Mine",  state[step].messagesImage);
             showImage("Overlay", state[step].overlayImage);
             showImage("Backdrop", state[step].background);
         } else {
             //  reset
             step = 0;
             clearText('summaryTextArea');
         }
     };

     instance.liveSend = function() {
         showImage("Mine", 'RQ-EIP-Sending-Live.gif');
         showImage("Backdrop", 'RQ-EIP-Background.png');
     };

     instance.liveSubmission = function(command, sendResult) {
        showImage("Backdrop", 'RQ-EIP-Background.png');
        showText('commandsTextArea', '\r\nSent command:[' + command + '] in #' + sendResult.MessageId);
        showImage("Mine", "RQ-EIP-Processing-Live.gif");
     };

     instance.liveSendingError = function(err) {
        showImage("Backdrop", 'RQ-EIP-Background.png');
        console.error(err);
        showImage("Mine", "RQ-EIP-SendingError.gif");
     };

     instance.liveReceive = function(message) {
        showImage("Backdrop", 'RQ-EIP-Background.png');
        showText('resultsTextArea', '\r\nResult [' + message.Body + '] for #' + message.MessageAttributes['CorrelationId'].StringValue);
        showImage("Mine", "RQ-EIP-Receiving-Live.gif");
     };

     instance.liveReceiveError = function(err) {
         showImage("Backdrop", 'RQ-EIP-Background.png');
         console.error(err);
         showImage("Mine", "RQ-EIP-ReceivingError.gif");
     };

     instance.liveClientPolling = function() {
         showImage("Backdrop", 'RQ-EIP-Background.png');
         // showImage("Mine", "RQ-EIP-Polling-Live.gif");
     };

     instance.liveClientPollingDone = function() {
        // showImage("Mine", "empty.gif");
        showImage("Backdrop", 'RQ-EIP-Background.png');
     };

    /**
     * Create a cpd4aws instance.
     * @param awsAccountNumber
     * @param awsAccessKeyId
     * @param awsSecretKey
     * @param awsRegion
     * @returns {*}
     */
    staticMethods.create = function () {
        return instance;
    };

    return staticMethods;

}());
