package uk.co.badgersinfoil.chunkymonkey.ts;

import java.util.HashMap;
import java.util.Map;

public class StreamType {

	private static final Map<Integer, StreamType> TYPES = new HashMap<>();

	// TODO: fixup formatting and consistency of STREAM_TYPE_* constant names / descriptions

	// 0x00 reserved
	public static final StreamType ISO_11172_Video = def(0x01, "ISO_11172_Video");
	public static final StreamType H262 = def(0x02, "H262");
	public static final StreamType ISO_11172_Audio = def(0x03, "ISO_11172_Audio");
	public static final StreamType ISO_13818_3_Audio = def(0x04, "ISO_13818_3_Audio");
	public static final StreamType H222_0_private_sections = def(0x05, "H222_0_private_sections");
	public static final StreamType H222_0_PES_private_data = def(0x06, "H222_0_PES_private_data");
	public static final StreamType MHEG = def(0x07, "MHEG");
	public static final StreamType H222_0_DSM_CC = def(0x08, "H222_0_DSM_CC");
	public static final StreamType H222_1 = def(0x09, "H222_1");
	public static final StreamType ISO_13818_6_Multiprotocol_Encapsulation = def(0x0A, "ISO_13818_6_Multiprotocol_Encapsulation");
	public static final StreamType DSMCC_UN_Messages = def(0x0B, "DSMCC_UN_Messages");
	public static final StreamType DSMCC_Stream_Descriptors = def(0x0C, "DSMCC_Stream_Descriptors");
	public static final StreamType DSMCC_Sections = def(0x0D, "DSMCC_Sections");
	public static final StreamType H222_0_auxiliary = def(0x0E, "H222_0_auxiliary");
	public static final StreamType ADTS = def(0x0F, "ADTS");
	public static final StreamType ISO_14496_2_Visual = def(0x10, "ISO_14496_2_Visual");
	public static final StreamType LATM = def(0x11, "LATM");
	public static final StreamType FlexMux_PES = def(0x12, "FlexMux_PES");
	public static final StreamType FlexMux_ISO14496_sections = def(0x13, "FlexMux_ISO14496_sections");
	public static final StreamType Synchronized_Download_Protocol = def(0x14, "Synchronized_Download_Protocol");
	public static final StreamType Metadata_in_PES = def(0x15, "Metadata_in_PES");
	public static final StreamType Metadata_in_metadata_sections = def(0x16, "Metadata_in_metadata_sections");
	public static final StreamType DSMCC_Data_Carousel_metadata = def(0x17, "DSMCC_Data_Carousel_metadata");
	public static final StreamType DSMCC_Object_Carousel_metadata = def(0x18, "DSMCC_Object_Carousel_metadata");
	public static final StreamType Synchronized_Download_Protocol_metadata = def(0x19, "Synchronized_Download_Protocol_metadata");
	public static final StreamType IPMP = def(0x1a, "IPMP");
	public static final StreamType H264 = def(0x1b, "H264");
	// 0x1c-0x23 reserved
	public static final StreamType H265 = def(0x24, "H265");
	// 0x26-0x41 reserved
	public static final StreamType Chinese_Video_Standard = def(0x42, "Chinese_Video_Standard");
	// 0x43-0x7f reserved
	// 0x80 privately defined
	public static final StreamType ATSC_Dolby_Digital_audio = def(0x81, "ATSC_Dolby_Digital_audio");
	// 0x82-0x94 privately defined
	public static final StreamType ATSC_DSMCC_Network_Resources_table = def(0x95, "ATSC_DSMCC_Network_Resources_table");
	// 0x95-0xc1 privately defined
	public static final StreamType ATSC_DSMCC_synchronous_data = def(0xc2, "ATSC_DSMCC_synchronous_data");
	// 0xc3-0xff privately defined

	private int type;
	private String name;

	public StreamType(int type, String name) {
		this.type = type;
		this.name = name;
	}

	// TODO: think about letting external code add definitions, while
	//       still being able to support object-identity-based equality

	private static StreamType def(int type, String name) {
		StreamType st = new StreamType(type, name);
		TYPES.put(type, st);
		return st;
	}

	public static StreamType forIndex(int i) {
		StreamType t = TYPES.get(i);
		if (t == null) {
			t = new StreamType(i, "Unknown");
		}
		return t;
	}

	public String getName() {
		return name;
	}
	public int getType() {
		return type;
	}

	@Override
	public String toString() {
		return "StreamType:"+name+"("+type+")";
	}
}
