package software.amazon.lambda.powertools.sqs.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import software.amazon.lambda.powertools.sqs.SqsLargeMessage;

public class SqsMessageHandler implements RequestHandler<SQSEvent, String> {

    @Override
    @SqsLargeMessage
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        return sqsEvent.getRecords().get(0).getBody();
    }
}