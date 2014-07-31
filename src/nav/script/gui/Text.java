package nav.script.gui;

import java.util.ArrayList;
import java.util.List;

public class Text {

	//

	public static String normalizeLines(String s) {
		s = replace(s, "\r\n", "\n");
		s = replace(s, "\r", "\n");
		return s;
	}

	public static String[] splitOnLines(String s) {
		return normalizeLines(s).split("\n");
	}

	public static String[] splitPair(String s, String find) {
		int io = s.indexOf(find);
		if (io == -1)
			return null;
		return new String[] { s.substring(0, io), s.substring(io + find.length()) };
	}

	//

	public static String before(String value, char find) {
		return before(value, find, null);
	}

	public static String before(String value, char find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(0, io);
	}

	public static String before(String value, String find) {
		return before(value, find, null);
	}

	public static String before(String value, String find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(0, io);
	}

	public static String after(String value, char find) {
		return after(value, find, null);
	}

	public static String after(String value, char find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(io + 1);
	}

	public static String after(String value, String find) {
		return after(value, find, null);
	}

	public static String after(String value, String find, String orElse) {
		int io = value.indexOf(find);
		return (io == -1) ? orElse : value.substring(io + find.length());
	}

	public static String removeLast(String value, char find) {
		int indexOf = value.lastIndexOf(find);
		if (indexOf != -1) {
			String before = value.substring(0, indexOf);
			String after = value.substring(indexOf + 1);
			value = before + after;
		}
		return value;
	}

	public static String trimToNull(String s) {
		if (s == null)
			return null;
		s = s.trim();
		if (s.isEmpty())
			return null;
		return s;
	}

	public static String between(String s, String after, String before) {
		return Text.before(Text.after(s, after), before);
	}

	// ----

	public static List<String> split(String s, char find) {
		List<String> list = new ArrayList<>();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if (io == -1)
				break;
			list.add(s.substring(off, io));
			off = io + 1;
		}
		list.add(s.substring(off));
		return list;
	}

	public static List<String> split(String s, String find) {
		List<String> list = new ArrayList<>();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if (io == -1)
				break;
			list.add(s.substring(off, io));
			off = io + find.length();
		}
		list.add(s.substring(off));
		return list;
	}

	public static String replace(String s, String find, String replace) {
		StringBuilder sb = new StringBuilder();
		int off = 0;
		while (true) {
			int io = s.indexOf(find, off);
			if (io == -1)
				break;
			sb.append(s.substring(off, io));
			sb.append(replace);
			off = io + find.length();
		}
		return sb.append(s.substring(off)).toString();
	}

	public static String removeDuplicates(String s, char c) {
		StringBuilder sb = new StringBuilder();
		boolean isAt = false;
		for (char x : s.toCharArray()) {
			if (x == c) {
				if (isAt)
					continue;
				isAt = true;
			} else {
				isAt = false;
			}
			sb.append(x);
		}
		return sb.toString();

	}
}