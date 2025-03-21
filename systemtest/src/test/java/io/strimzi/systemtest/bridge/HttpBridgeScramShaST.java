/*
 * Copyright 2019, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.bridge;

import io.strimzi.api.kafka.model.CertSecretSource;
import io.strimzi.api.kafka.model.KafkaResources;
import io.strimzi.api.kafka.model.KafkaUser;
import io.strimzi.api.kafka.model.PasswordSecretSource;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.listener.KafkaListenerTls;
import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.HttpBridgeBaseST;
import io.strimzi.systemtest.utils.StUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Random;

import static io.strimzi.systemtest.Constants.BRIDGE;
import static io.strimzi.systemtest.Constants.NODEPORT_SUPPORTED;
import static io.strimzi.systemtest.Constants.REGRESSION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(BRIDGE)
@Tag(REGRESSION)
@Tag(NODEPORT_SUPPORTED)
@ExtendWith(VertxExtension.class)
class HttpBridgeScramShaST extends HttpBridgeBaseST {
    private static final Logger LOGGER = LogManager.getLogger(HttpBridgeScramShaST.class);
    private static final String NAMESPACE = "bridge-cluster-test-scram-sha";

    private String bridgeHost = "";
    private int bridgePort = Constants.HTTP_BRIDGE_DEFAULT_PORT;
    private String userName = "bob";

    @Test
    void testSendSimpleMessageTlsScramSha() throws Exception {
        int messageCount = 50;
        String topicName = "topic-simple-send-" + new Random().nextInt(Integer.MAX_VALUE);
        // Create topic
        testClassResources().topic(CLUSTER_NAME, topicName).done();

        JsonObject records = generateHttpMessages(messageCount);
        JsonObject response = sendHttpRequests(records, bridgeHost, bridgePort, topicName);
        checkSendResponse(response, messageCount);
        receiveMessagesExternalScramSha(NAMESPACE, topicName, messageCount, userName);
    }

    @Test
    void testReceiveSimpleMessageTlsScramSha() throws Exception {
        int messageCount = 50;
        String topicName = "topic-simple-receive-" + new Random().nextInt(Integer.MAX_VALUE);
        // Create topic
        testClassResources().topic(CLUSTER_NAME, topicName).done();
        // Send messages to Kafka
        sendMessagesExternalScramSha(NAMESPACE, topicName, messageCount, userName);

        String name = "kafka-consumer-simple-receive";
        String groupId = "my-group-" + new Random().nextInt(Integer.MAX_VALUE);

        JsonObject config = new JsonObject();
        config.put("name", name);
        config.put("format", "json");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Create consumer
        JsonObject response = createBridgeConsumer(config, bridgeHost, bridgePort, groupId);
        assertThat("Consumer wasn't created correctly", response.getString("instance_id"), is(name));
        // Create topics json
        JsonArray topic = new JsonArray();
        topic.add(topicName);
        JsonObject topics = new JsonObject();
        topics.put("topics", topic);
        // Subscribe
        assertTrue(subscribeHttpConsumer(topics, bridgeHost, bridgePort, groupId, name));
        // Try to consume messages
        JsonArray bridgeResponse = receiveHttpRequests(bridgeHost, bridgePort, groupId, name);
        if (bridgeResponse.size() == 0) {
            // Real consuming
            bridgeResponse = receiveHttpRequests(bridgeHost, bridgePort, groupId, name);
        }

        assertThat("Sent message count is not equal with received message count", bridgeResponse.size(), is(messageCount));
        // Delete consumer
        assertTrue(deleteConsumer(bridgeHost, bridgePort, groupId, name));
    }

    @BeforeAll
    void createClassResources() throws InterruptedException {
        LOGGER.info("Deploy Kafka and Kafka Bridge before tests");

        KafkaListenerAuthenticationTls auth = new KafkaListenerAuthenticationTls();
        KafkaListenerTls listenerTls = new KafkaListenerTls();
        listenerTls.setAuth(auth);

        // Deploy kafka
        testClassResources().kafkaEphemeral(CLUSTER_NAME, 1, 1)
                .editSpec()
                .editKafka()
                .withNewListeners()
                .withNewKafkaListenerExternalNodePort()
                .withTls(true)
                .withAuth(new KafkaListenerAuthenticationScramSha512())
                .endKafkaListenerExternalNodePort()
                .withNewTls().withAuth(new KafkaListenerAuthenticationScramSha512()).endTls()
                .endListeners()
                .endKafka()
                .endSpec().done();

        // Create Kafka user
        KafkaUser userSource = testClassResources().scramShaUser(CLUSTER_NAME, userName).done();
        StUtils.waitForSecretReady(userName);

        // Initialize PasswordSecret to set this as PasswordSecret in Mirror Maker spec
        PasswordSecretSource passwordSecret = new PasswordSecretSource();
        passwordSecret.setSecretName(userName);
        passwordSecret.setPassword("password");

        // Initialize CertSecretSource with certificate and secret names for consumer
        CertSecretSource certSecret = new CertSecretSource();
        certSecret.setCertificate("ca.crt");
        certSecret.setSecretName(clusterCaCertSecretName(CLUSTER_NAME));

        // Deploy http bridge
        testClassResources().kafkaBridge(CLUSTER_NAME, KafkaResources.tlsBootstrapAddress(CLUSTER_NAME), 1, Constants.HTTP_BRIDGE_DEFAULT_PORT)
            .editSpec()
            .withNewKafkaBridgeAuthenticationScramSha512()
                .withNewUsername(userName)
                .withPasswordSecret(passwordSecret)
            .endKafkaBridgeAuthenticationScramSha512()
                .withNewTls()
                .withTrustedCertificates(certSecret)
                .endTls()
            .endSpec().done();

        deployBridgeNodePortService();
        bridgePort = getBridgeNodePort();
        bridgeHost = kubeClient(NAMESPACE).getNodeAddress();
    }

    @Override
    public String getBridgeNamespace() {
        return NAMESPACE;
    }
}
