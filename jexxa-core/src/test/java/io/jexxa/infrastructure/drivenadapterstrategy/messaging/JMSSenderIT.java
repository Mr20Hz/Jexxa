package io.jexxa.infrastructure.drivenadapterstrategy.messaging;


import static io.jexxa.TestConstants.JEXXA_APPLICATION_SERVICE;
import static io.jexxa.TestConstants.JEXXA_DRIVEN_ADAPTER;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;

import io.jexxa.TestConstants;
import io.jexxa.application.domain.valueobject.JexxaValueObject;
import io.jexxa.core.JexxaMain;
import io.jexxa.infrastructure.drivingadapter.messaging.JMSAdapter;
import io.jexxa.infrastructure.utils.messaging.QueueListener;
import io.jexxa.infrastructure.utils.messaging.TopicListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Tag(TestConstants.INTEGRATION_TEST)
class JMSSenderIT
{
    private final JexxaValueObject message = new JexxaValueObject(42);

    private TopicListener topicListener;
    private QueueListener queueListener;
    private JexxaMain jexxaMain;

    private JMSSender objectUnderTest;

    @BeforeEach
    void initTests()
    {
        jexxaMain = new JexxaMain(JMSSenderIT.class.getSimpleName());
        topicListener = new TopicListener();
        queueListener = new QueueListener();
        objectUnderTest = new JMSSender(jexxaMain.getProperties());

        jexxaMain.addToApplicationCore(JEXXA_APPLICATION_SERVICE)
                .addToInfrastructure(JEXXA_DRIVEN_ADAPTER)
                .bind(JMSAdapter.class).to(queueListener)
                .bind(JMSAdapter.class).to(topicListener)
                .start();
    }

    @Test
    void sendMessageToTopic()
    {
        //Arrange --

        //Act
        objectUnderTest.sendToTopic(message, TopicListener.TOPIC_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !topicListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }


    @Test
    void sendMessageToQueue()
    {
        //Arrange --

        //Act
        objectUnderTest.sendToQueue(message, QueueListener.QUEUE_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !queueListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageReconnectQueue() throws JMSException
    {
        //Arrange --

        //Act (simulate an error in between sending two messages
        objectUnderTest.sendToQueue(message, QueueListener.QUEUE_DESTINATION);
        simulateConnectionException(objectUnderTest.getConnection());
        objectUnderTest.sendToQueue(message, QueueListener.QUEUE_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> queueListener.getMessages().size() >= 2);

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    private void simulateConnectionException(Connection connection) throws JMSException
    {
        var listener = connection.getExceptionListener();

        connection.close();

        listener.onException(new JMSException("Simulated error "));
    }
}