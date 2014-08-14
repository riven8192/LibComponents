package nav.script.road;

public class RoadTile {
	public static enum Dir {
		NORTH, EAST, SOUTH, WEST
	}

	public final String name;
	public final int x, y;

	public RoadTile(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = y;
	}

	private int usage = 0x0000;

	public boolean isEmpty(int bit) {
		return (usage & (1 << bit)) == 0;
	}

	public void enter(int bit) {
		if(!this.isEmpty(bit))
			throw new IllegalStateException();
		usage |= (1 << bit);
	}

	public void leave(int bit) {
		if(this.isEmpty(bit))
			throw new IllegalStateException();
		usage &= ~(1 << bit);
	}

	public static int[] getBits(Dir a, Dir b) {
		if(a == b)
			return DIR_CELLS[a.ordinal()];
		return DIRDIR_CELLS[a.ordinal()][b.ordinal()];
	}

	private static int[][] DIR_CELLS = new int[Dir.values().length][];
	static {
		DIR_CELLS[Dir.NORTH.ordinal()] = new int[] { 15, 11, 7, 3 };
		DIR_CELLS[Dir.EAST.ordinal()] = new int[] { 12, 13, 14, 15 };
		DIR_CELLS[Dir.SOUTH.ordinal()] = new int[] { 0, 4, 8, 12 };
		DIR_CELLS[Dir.WEST.ordinal()] = new int[] { 3, 2, 1, 0 };
	}

	private static int[][][] DIRDIR_CELLS = new int[Dir.values().length][Dir.values().length][];
	static {
		DIRDIR_CELLS[Dir.NORTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 15, 11, 7, 3 };
		DIRDIR_CELLS[Dir.NORTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 15, 11, 7, 3, 2, 1, 0 };
		DIRDIR_CELLS[Dir.SOUTH.ordinal()][Dir.EAST.ordinal()] = new int[] { 0, 4, 8, 12, 13, 14, 15 };
		DIRDIR_CELLS[Dir.SOUTH.ordinal()][Dir.WEST.ordinal()] = new int[] { 0, 4, 8, 12 };
		DIRDIR_CELLS[Dir.EAST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 12, 13, 14, 15, 11, 7, 3 };
		DIRDIR_CELLS[Dir.EAST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 12 };
		DIRDIR_CELLS[Dir.WEST.ordinal()][Dir.NORTH.ordinal()] = new int[] { 3 };
		DIRDIR_CELLS[Dir.WEST.ordinal()][Dir.SOUTH.ordinal()] = new int[] { 3, 7, 11, 15 };
	}

	@Override
	public String toString() {
		return name;
	}
}
