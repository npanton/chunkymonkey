package uk.co.badgersinfoil.chunkymonkey.event;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.co.badgersinfoil.chunkymonkey.event.Reporter.LogFormat;

public class EventFormatter {

	// TODO: I'm sure I've seen something like java.text.MessageFormat,
	//       but with named substitutions; however having been unable to
	//       find it, I hacked-together this thing.

	public String format(Event event, LogFormat logFormat) {
		StringBuffer sb = new StringBuffer();
		Pattern p = Pattern.compile("\\{(.+?)\\}");
		Matcher m = p.matcher(logFormat.value());
		Set<String> matched = new HashSet<>();
		Set<String> unmatched = new HashSet<>();
		while (m.find()) {
			String name = m.group(1);
			Object val = event.attr(name);
			if (val == null) {
				unmatched.add(name);
			} else {
				m.appendReplacement(sb, String.valueOf(val));
				matched.add(name);
			}
		}
		m.appendTail(sb);
		for (String name : event.attrs().keySet()) {
			if (!matched.contains(name)) {
				sb.append(" ").append(name)
				  .append("=").append(String.valueOf(event.attr(name)));
			}
		}
		return sb.toString();
	}
}
