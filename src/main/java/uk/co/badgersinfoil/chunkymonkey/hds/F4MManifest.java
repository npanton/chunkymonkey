package uk.co.badgersinfoil.chunkymonkey.hds;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Manifest;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Media;

public class F4MManifest {

	private URI manifestUri;
	private Manifest manifest;

	public F4MManifest(URI manifestUri, Manifest unmarshal) {
		this.manifestUri = manifestUri;
		this.manifest = unmarshal;
	}

	public List<F4MMedia> getMedia() {
		List<F4MMedia> result = new ArrayList<F4MMedia>();
		for (Media m : manifest.getMedia()) {
			result.add(new F4MMedia(manifest, m));
		}
		return result;
	}
}
