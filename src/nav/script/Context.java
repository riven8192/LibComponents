package nav.script;

public interface Context {
	public State signal(String[] words);

	public boolean query(String var);

	public void nextSleep(String time);
}