package kafka.tutorial1;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerDemo {
    public static void main(String[] args) {
        String bootstrapServers = "127.0.0.1:9092";

        // Create Producer properties.
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Create the Producer.
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        // Create a Producer Record.
        ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "nice to see you");

        // Send data - Asynchronous.
        producer.send(record);

        // Flush data.
        producer.flush();
        // Flush and Close Producer.
        producer.close();
    }
}