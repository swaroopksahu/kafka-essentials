package kafka.tutorial2;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    String consumerKey = "bFxwoCMOUBZRsXpjMdUKZ1CaI";  // API Key
    String consumerSecret = "9IatvHEWQaSp27nAlyKZ9KQyfLca0pn4hFA6sJD3VITCyq9773"; // API Secret Key
    String token = "2509122026-I5VwdVEgl89lQGhgayqFyeIFi6bKREcN0vokNx3"; // Access Token
    String secret = "I2Vvn5cotyZ1VLIk88C6bc4xWpMulgURE2j6eFTbJJbbZ"; // Access Token Secret

    List<String> terms = Lists.newArrayList("kafka", "java", "politics", "usa", "india","a", "ronaldo");

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    private void run() {
        logger.info("Setup");

        // Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream.
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);

        // Create a Twitter Client.
        Client client = createTwitterClient(msgQueue);

        // Attempt to establish a connection.
        client.connect();

        // Create a Kafka Producer.
        KafkaProducer<String, String> producer = createKafkaProducer();

        // Add a shutdown hook.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Stopping application...");
            logger.info("Shutting down client from Twitter...");
            client.stop();
            logger.info("Closing producer...");
            producer.close();
            logger.info("Done!");
        }));

        // Loop to send tweets to Kafka.
        // On a different thread, or multiple different threads....
        while(!client.isDone()){
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
            if(msg != null){
                logger.info(msg);
                producer.send(new ProducerRecord<>("twitter_tweets", null, msg), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if (e != null){
                            logger.error("Something bad happened", e);
                        }
                    }
                });

            }
        }
        logger.info("End of application");
    }

    private Client createTwitterClient(BlockingQueue<String> msgQueue) {
        // Declare the host you want to connect to, the end point, and authentication (basic auth or oauth).
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file.
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("hosebird-client-01")
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        String bootstrapServers = "127.0.0.1:9092";

        // Create Producer properties.
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Create Safe Producer.
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); // kafka 2.0 >= 1.1 so we can keep this as 5. Use 1 otherwise.

        // High throughput Producer (at the expense of a bit of latency and CPU usage).
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024)); // 32KB batch size

        // Create the Producer.
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        return producer;
    }
}