package uk.co.badgersinfoil.chunkymonkey.rfc6381;

import java.util.List;
import org.junit.Test;
import uk.co.badgersinfoil.chunkymonkey.h264.H264Profile;
import uk.co.badgersinfoil.chunkymonkey.ts.AudioObjectType;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class CodecsParserTest {

	@Test
	public void testParse() throws Exception {
		// given,
		OtiParser aotParser = new AudioObjectTypeParser();
		Mp4aCodecParser mp4aCodecParser = new Mp4aCodecParser();
		mp4aCodecParser.addParser("40", aotParser);
		CodecsParser parser = new CodecsParser();
		parser.addParser("mp4a", mp4aCodecParser);
		Avc1CodecParser avc1CodecParser = new Avc1CodecParser();
		parser.addParser("avc1", avc1CodecParser);

		// when,
		List<Rfc6381Codec> codecs = parser.parseCodecs("mp4a.40.2,avc1.66.30");

		// then,
		assertEquals(2, codecs.size());
		Rfc6381Codec codec0 = codecs.get(0);
		assertEquals("mp4a.40.2", codec0.toString());
		assertThat(codec0, instanceOf(AudioObjectTypeCodec.class));
		assertEquals(((AudioObjectTypeCodec)codec0).getType(), AudioObjectType.AAC_LOW_COMPLEXITY);

		Rfc6381Codec codec1 = codecs.get(1);
		assertEquals("avc1.66.30", codec1.toString());
		assertThat(codec1, instanceOf(Avc1Codec.class));
		Avc1Codec avcCodec = (Avc1Codec)codec1;
		assertEquals(H264Profile.BASELINE, avcCodec.getProfile());
		assertNull(avcCodec.getConstraints());
		assertEquals(30, avcCodec.getLevel());
	}
}
