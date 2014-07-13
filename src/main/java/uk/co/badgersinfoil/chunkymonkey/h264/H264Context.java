package uk.co.badgersinfoil.chunkymonkey.h264;

import java.util.HashMap;
import java.util.Map;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.event.Locator;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer.ParseState;
import uk.co.badgersinfoil.chunkymonkey.h264.H264PesConsumer.PesNalUnitLocator;
import uk.co.badgersinfoil.chunkymonkey.h264.NALUnit.UnitType;
import uk.co.badgersinfoil.chunkymonkey.h264.NalUnitConsumer.NalUnitContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;

public class H264Context implements ElementryContext {
	private MediaContext parentContext;
	private boolean ignoreRest;
	private PESPacket pesPacket;
	private int unitIndex = 0;
	private SeqParamSet lastSeqParamSet;
	private PicParamSet lastPicParamSet;
	private boolean nalStarted;
	private NALUnit nalUnit;
	private ParseState parseState;
	private NalUnitConsumer currentConsumer;
	private NalUnitContext currentNalContext;
	private boolean continuityError;
	private Map<UnitType,NalUnitContext> nalUnitContexts = new HashMap<>();
	private NalUnitContext defaultNalContext;

	public H264Context(MediaContext parentContext) {
		this.parentContext = parentContext;
	}
	public void addNalContext(UnitType type, NalUnitConsumer consumer) {
		nalUnitContexts.put(type, consumer.createContext(this));
	}
	public void setDefaultNalContext(NalUnitContext defaultNalContext) {
		this.defaultNalContext = defaultNalContext;
	}
	NalUnitContext nalContext(UnitType type) {
		NalUnitContext context = nalUnitContexts.get(type);
		return context == null ? defaultNalContext : context;
	}

	public boolean isIgnoreRest() {
		return ignoreRest;
	}
	public void setIgnoreRest(boolean ignoreRest) {
		this.ignoreRest = ignoreRest;
	}
	public PESPacket getPesPacket() {
		return pesPacket;
	}
	public void setPesPacket(PESPacket pesPacket) {
		this.pesPacket = pesPacket;
	}
	public int incUnitIndex() {
		return unitIndex++;
	}
	public void start() {
		parseState = ParseState.START;
		ignoreRest = false;
		continuityError = false;
		unitIndex = 0;
		nalStarted = false;
	}
	public void lastSeqParamSet(SeqParamSet params) {
		this.lastSeqParamSet = params;
	}
	public SeqParamSet lastSeqParamSet() {
		return lastSeqParamSet;
	}
	public void lastPicParamSet(PicParamSet params) {
		lastPicParamSet = params;
	}
	public PicParamSet lastPicParamSet() {
		return lastPicParamSet;
	}
	public boolean nalStarted() {
		return nalStarted;
	}
	public void nalStarted(boolean b) {
		nalStarted = b;
	}
	public void setNalUnit(NALUnit u) {
		nalUnit = u;
	}
	public NALUnit getNalUnit() {
		return nalUnit;
	}
	public ParseState state() {
		return parseState;
	}
	public void state(ParseState state) {
		parseState = state;
	}
	public void setCurrentNalUnitConsumer(NalUnitConsumer consumer) {
		this.currentConsumer = consumer;
	}
	public NalUnitConsumer getCurrentNalUnitConsumer() {
		return currentConsumer;
	}
	public void setCurrentNalUnitContext(NalUnitContext nalCtx) {
		this.currentNalContext = nalCtx;
	}
	public NalUnitContext getCurrentNalContext() {
		return currentNalContext;
	}
	public void continuityError(boolean b) {
		continuityError = b;
	}
	public boolean continuityError() {
		return continuityError;
	}
	@Override
	public Locator getLocator() {
		return new PesNalUnitLocator(parentContext.getLocator(), unitIndex, nalUnit.nalUnitType());
	}

	public MediaContext getParentContext() {
		return parentContext;
	}
}
