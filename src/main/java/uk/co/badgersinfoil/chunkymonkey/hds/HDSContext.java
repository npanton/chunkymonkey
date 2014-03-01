package uk.co.badgersinfoil.chunkymonkey.hds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.BootstrapInfo;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Manifest;

public class HDSContext {
	private URI manifest;
	private HttpClient httpclient;

	public HDSContext(URI manifest) {
		this.manifest = manifest;
	}
	public void setHttpClient(HttpClient httpclient) {
		this.httpclient = httpclient;
	}
	
	public F4MManifest requestManifest() throws IOException {
		HttpGet req = new HttpGet(manifest);
		HttpResponse resp = httpclient.execute(req);
		InputStream stream = resp.getEntity().getContent();
		F4MLoader loader = new F4MLoader();
		try {
			Manifest m = loader.load(manifest, stream);
			return new F4MManifest(manifest, m);
		} catch (JAXBException e) {
			throw new IOException(e);
		} finally {
			stream.close();
		}
	}

	public F4VBootstrap requestBootstrap(F4MMedia media) throws IOException {
		BootstrapInfo info = media.getBootstrapInfo();
		URI bootstrapUri = manifest.resolve(info.getUrl());
System.out.println(bootstrapUri);
		HttpGet req = new HttpGet(bootstrapUri);
		HttpResponse resp = httpclient.execute(req);
		ByteBuf buffer = Unpooled.buffer();
		ByteBufOutputStream o = new ByteBufOutputStream(buffer);
		resp.getEntity().writeTo(o);
		return new F4VBootstrap(buffer);
	}
}

