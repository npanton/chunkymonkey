package uk.co.badgersinfoil.chunkymonkey.ts;

import uk.co.badgersinfoil.chunkymonkey.Locator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ProgramMapTable {

	private ByteBuf buf = Unpooled.buffer();
	private Locator locator;

	public ProgramMapTable(Locator locator, ByteBuf buf) {
		this.locator = locator;
		appendPayload(buf);
	}

	public int pointer() {
		return buf.getByte(0) & 0b111111;
	}

	public int tableId() {
		return buf.getByte(1+pointer()) & 0b11111111;
	}
	public int sectionSyntaxIndicator() {
		return (buf.getByte(2+pointer()) & 0b10000000) >> 7;
	}
	public int sectionLength() {
		return (buf.getByte(2+pointer()) & 0b00001111) << 8
		      |(buf.getByte(3+pointer()) & 0b11111111);
	}
	public int transportStreamId() {
		return (buf.getByte(4+pointer()) & 0b11111111) << 8
		      |(buf.getByte(5+pointer()) & 0b11111111);
	}
	public int versionNumber() {
		return (buf.getByte(6+pointer()) & 0b00111110) >> 1;
	}
	public boolean currentNextIndicator() {
		return 0 != (buf.getByte(6+pointer()) & 0b1);
	}
	public int sectionNumber() {
		return buf.getByte(7+pointer()) & 0b11111111;
	}
	public int lastSectionNumber() {
		return buf.getByte(8+pointer()) & 0b11111111;
	}
	public int pcrPID() {
		return (buf.getByte(9+pointer())  & 0b00011111) << 8
		      |(buf.getByte(10+pointer()) & 0b11111111);
	}
	public int programInfoLength() {
		return (buf.getByte(11+pointer()) & 0b00001111) << 8
		      |(buf.getByte(12+pointer()) & 0b11111111);
	}
	public ProgramDescriptorIterator programDescriptors() {
		return new ProgramDescriptorIterator();
	}

	public class ProgramDescriptorIterator {
		private int i = 0;
		public boolean hasNext() {
			return i < programInfoLength();
		}
		public void next() {
			final int TAG_AND_LENGTH_HEADER_SIZE = 2;
			i += descriptorLength()+TAG_AND_LENGTH_HEADER_SIZE;
		}
		public int descriptorTag() {
			return buf.getByte(13+pointer()+i) & 0xff;
		}
		public int descriptorLength() {
			return buf.getByte(13+pointer()+i+1) & 0xff;
		}
		// TODO: expose rest of descriptor info
	}

	public StreamDescriptorIterator streamDescriptors() {
		return new StreamDescriptorIterator();
	}

	public class StreamDescriptorIterator {
		private int offset = 13+pointer()+programInfoLength();
		private int i = 0;
		public boolean hasNext() {
			final int CRC_LENGTH = 4;
			return offset < sectionLength() - CRC_LENGTH ;
		}
		public void next() {
			offset += 5 + esInfoLength();
			i++;
		}
		public StreamType streamType() {
			return StreamType.forIndex(buf.getByte(offset) & 0xff);
		}
		public int elementryPID() {
			return (buf.getByte(offset+1) & 0b00011111) << 8
			      |(buf.getByte(offset+2) & 0b11111111);
		}
		private int esInfoLength() {
			return (buf.getByte(offset+3) & 0b00001111) << 8
			      |(buf.getByte(offset+4) & 0b11111111);
		}
		public Locator getLocator() {
			return new Locator() {
				private int index = i;
				@Override
				public String toString() {
					return "PMT stream-descriptor #"+index+"\n"
							+"  at "+getParent();
				}
				@Override
				public Locator getParent() {
					return ProgramMapTable.this.getLocator();
				}
			};
		}
		public void toString(StringBuilder b) {
			b.append(" streamType=").append(streamType())
			 .append(";elementryPID=").append(elementryPID())
			 .append(";esInfoLength=").append(esInfoLength());
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("pointer=").append(pointer())
		 .append(" tableId=").append(tableId())
		 .append(" sectionSyntaxIndicator=").append(sectionSyntaxIndicator())
		 .append(" sectionLength=").append(sectionLength())
		 .append(" transportStreamId=").append(transportStreamId())
		 .append(" versionNumber=").append(versionNumber())
		 .append(" currentNextIndicator=").append(currentNextIndicator())
		 .append(" sectionNumber=").append(sectionNumber())
		 .append(" lastSectionNumber=").append(lastSectionNumber())
		 .append(" pcrPID=").append(pcrPID())
		 .append(" programInfoLength=").append(programInfoLength())
		 .append(" programDescriptors={");
		ProgramDescriptorIterator i = programDescriptors();
		while (i.hasNext()) {
			b.append(" tag=").append(i.descriptorTag())
			 .append(";len=").append(i.descriptorLength());
			i.next();
		}
		b.append(" }")
		 .append(" streamDescriptors={");
		StreamDescriptorIterator j = streamDescriptors();
		while (j.hasNext()) {
			j.toString(b);
			j.next();
		}
		b.append(" }");
		return b.toString();
	}

	public Locator getLocator() {
		return locator;
	}

	public boolean isComplete() {
		return sectionLength() <= buf.readableBytes();
	}

	public void appendPayload(ByteBuf payload) {
		buf.writeBytes(payload);
	}
}
