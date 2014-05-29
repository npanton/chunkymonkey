package uk.co.badgersinfoil.chunkymonkey.snickersnack;

import io.netty.buffer.ByteBuf;
import uk.co.badgersinfoil.chunkymonkey.MediaContext;
import uk.co.badgersinfoil.chunkymonkey.ts.ElementryContext;
import uk.co.badgersinfoil.chunkymonkey.ts.PESConsumer;
import uk.co.badgersinfoil.chunkymonkey.ts.PESPacket;
import uk.co.badgersinfoil.chunkymonkey.ts.TSPacket;

public class PesSwitchConsumer implements PESConsumer {

	private PESConsumer delegate;
	private boolean enabled;

	public PesSwitchConsumer(PESConsumer delegate) {
		this.delegate = delegate;
	}

	@Override
	public void start(ElementryContext ctx, PESPacket pesPacket) {
		if (enabled) {
			delegate.start(ctx, pesPacket);
		}
	}

	@Override
	public void continuation(ElementryContext ctx, TSPacket packet,
			ByteBuf payload)
	{
		if (enabled) {
			delegate.continuation(ctx, packet, payload);
		}
	}

	@Override
	public void end(ElementryContext ctx) {
		if (enabled) {
			delegate.end(ctx);
		}
	}

	@Override
	public void continuityError(ElementryContext ctx) {
		if (enabled) {
			delegate.continuityError(ctx);
		}
	}

	public boolean enabled() {
		return enabled;
	}
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public ElementryContext createContext(MediaContext parentContext) {
		return delegate.createContext(parentContext);
	}
}
