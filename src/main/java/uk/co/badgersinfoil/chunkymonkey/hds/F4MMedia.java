package uk.co.badgersinfoil.chunkymonkey.hds;

import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.BootstrapInfo;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Manifest;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Media;

public class F4MMedia {

	private Manifest manifest;
	private Media media;

	public F4MMedia(Manifest manifest, Media m) {
		this.manifest = manifest;
		this.media = m;
	}

	public BootstrapInfo getBootstrapInfo() {
		String id = media.getBootstrapInfoId();
		for (BootstrapInfo info : manifest.getBootstrapInfo()) {
			if ((info.getId() == null && id == null) || (id != null && id.equals(info.getId()))) {
				return info;
			}
		}
		return null;
	}

}
