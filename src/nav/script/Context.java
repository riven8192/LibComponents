package nav.script;

public interface Context {
	public State signal(String text);

	public boolean query(String var);

	public void nextSleep(String time);
}