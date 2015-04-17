package be.vrt.services.logging.log.consumer.appender;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class ElasticSearchAppender extends AbstractJsonAppender {

	private Client client;

	@Override
	public void start() {
		super.start();
		client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
	}

	@Override
	protected void persist(String json) {
		client.prepareIndex("logging", "log")
				.setSource(json)
				.execute()
				.actionGet();
	}

	@Override
	public void stop() {
		super.stop();
		client.close();
	}

}