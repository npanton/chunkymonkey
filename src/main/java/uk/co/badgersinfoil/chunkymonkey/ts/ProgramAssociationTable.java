package uk.co.badgersinfoil.chunkymonkey.ts;

import io.netty.buffer.ByteBuf;

public class ProgramAssociationTable {

	private ByteBuf buf;

	public ProgramAssociationTable(ByteBuf buf) {
		this.buf = buf;
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
	private int entryCount() {
		return (sectionLength()-9)/4;
	}
	static enum ProgramEntryKind {
		NETWORK, PROGRAM_MAP
		
	}
	public class ProgramEntry {
		private int i = -1;
		public boolean next() {
			i++;
			return i < entryCount();
		}
		public int programNumber() {
			final int o = 9+i*4;
			return (buf.getByte(o)   & 0xff) << 1
			      |(buf.getByte(o+1) & 0xff);
		}
		public ProgramAssociationTable.ProgramEntryKind kind() {
			return programNumber() == 0 ? ProgramEntryKind.NETWORK : ProgramEntryKind.PROGRAM_MAP;
		}
		public int reserved() {
			return (buf.getByte(9+i*4+2) & 0b11100000) >> 5;
		}
		private int val() {
			return (buf.getByte(9+i*4+2) & 0b00011111) << 8
			      |(buf.getByte(9+i*4+3) & 0b11111111);
		}
		public int networkPid() {
			if (kind() != ProgramEntryKind.NETWORK) {
				throw new RuntimeException("This is a program_map_PID entry, not a network_PID entry");
			}
			return val();
		}
		public int programMapPid() {
			if (kind() != ProgramEntryKind.PROGRAM_MAP) {
				throw new RuntimeException("This is a network_PID entry, not a program_map_PID entry");
			}
			return val();
		}
	}
	public ProgramEntry entries() {
		return new ProgramEntry();
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
		 .append(" entries={");
		ProgramEntry entry = entries();
		while (entry.next()) {
			switch (entry.kind()) {
			case NETWORK:
				b.append(" Network=").append(entry.networkPid());
				break;
			case PROGRAM_MAP:
				b.append(" Program[").append(entry.programNumber()).append("]=").append(entry.programMapPid());
				break;
			}
		}
		b.append(" }");
		return b.toString();
	}
}