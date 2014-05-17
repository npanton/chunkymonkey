package uk.co.badgersinfoil.chunkymonkey.rtp;

import uk.co.badgersinfoil.chunkymonkey.rtp.RtpParser.RtpContext;
import uk.co.badgersinfoil.chunkymonkey.ts.TSContext;

public interface RtpConsumer {

	void packet(TSContext ctx, RtpPacket p);

	TSContext createContext(RtpContext ctx);
}
