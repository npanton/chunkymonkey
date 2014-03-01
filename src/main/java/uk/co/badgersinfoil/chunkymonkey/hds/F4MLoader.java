package uk.co.badgersinfoil.chunkymonkey.hds;

import java.io.InputStream;
import java.net.URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import uk.co.badgersinfoil.chunkymonkey.source.hds.manifest.Manifest;

public class F4MLoader {
	public Manifest load(URI manifestUri, InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance("uk.co.badgersinfoil.chunkymonkey.source.hds.manifest");
		Unmarshaller u = jc.createUnmarshaller();
		return (Manifest)u.unmarshal(inputStream);
	}
}
