package uk.co.badgersinfoil.chunkymonkey.conformist;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.chilicat.m3u8.Element;
import net.chilicat.m3u8.PlayList;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import uk.co.badgersinfoil.chunkymonkey.hls.HLSContext;
import uk.co.badgersinfoil.chunkymonkey.ts.MultiTSPacketConsumer;

public class Main {

	public static void main(String[] args) throws Exception {
		URI uri = new URI(args[0]);
		AppBuilder b = new AppBuilder();
		MultiTSPacketConsumer tsConsumer = b.createConsumer();
		HLSContext context = new HLSContext(uri, tsConsumer);
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		context.setHttpClient(httpclient);
		try {
			context.consumeStream();
		} finally {
			httpclient.close();
		}
		System.out.println("Done");
	}
	
	private static Pattern seq = Pattern.compile("(\\d+)\\.ts$");

	private static void validatePlaylist(PlayList playlist) {
		long lastSeqNo = -1;
		Element lastElement = null;
		for (Element element : playlist) {
			Matcher m = seq.matcher(element.getURI().getPath());
			if (m.find()) {
				long seqNo = Long.parseLong(m.group(1));
				if (lastSeqNo != -1) {
					if (seqNo != lastSeqNo + 1) {
						System.err.println("Unexpcted sequence difference "+(seqNo-lastSeqNo)+" from "+element.getURI()+" to "+lastElement.getURI());
					}
				}
				lastSeqNo = seqNo;
			}
			lastElement = element;
		}
	}
}
