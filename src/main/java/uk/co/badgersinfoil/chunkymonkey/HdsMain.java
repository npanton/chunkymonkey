package uk.co.badgersinfoil.chunkymonkey;

import java.io.InputStream;
import java.net.URI;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import uk.co.badgersinfoil.chunkymonkey.hds.F4MLoader;
import uk.co.badgersinfoil.chunkymonkey.hds.F4MManifest;
import uk.co.badgersinfoil.chunkymonkey.hds.F4MMedia;
import uk.co.badgersinfoil.chunkymonkey.hds.F4VBootstrap;
import uk.co.badgersinfoil.chunkymonkey.hds.HDSContext;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.BootstrapInfo;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Manifest;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Media;

public class HdsMain {
	public static void main(String[] args) throws Exception {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		URI manifestUri = new URI("http://bbcfmhds.vo.llnwd.net/hds-live/livepkgr/_definst_/bbc2/bbc2_480.f4m");
		HDSContext ctx = new HDSContext(manifestUri);
		ctx.setHttpClient(httpclient);
		
		F4MManifest manifest = ctx.requestManifest();
		F4MMedia media = manifest.getMedia().get(0);
		F4VBootstrap bootstrap = ctx.requestBootstrap(media);
		System.out.println(bootstrap);
//		try {
//			Manifest m = loader.load(manifestUri, stream);
//			Media media = m.getMedia().get(0);
//			BootstrapInfo info = findBootstrapInfo(m, media);
//			System.out.println(info.getUrl());
//		} finally {
//			stream.close();
//		}
	}

	private static BootstrapInfo findBootstrapInfo(Manifest m, Media media) {
		String id = media.getBootstrapInfoId();
		for (BootstrapInfo info : m.getBootstrapInfo()) {
			if ((info.getId() == null && id == null) || (id != null && id.equals(info.getId()))) {
				return info;
			}
		}
		return null;
	}
}
