package nav.script.road;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import nav.util.Vec2;

public class RoadGridTest {
	public static void main(String[] args) {

		RoadTile t1 = new RoadTile("A", 5 + 0, 5 + 0);
		RoadTile t2 = new RoadTile("B", 5 + 1, 5 + 0);
		RoadTile t3 = new RoadTile("C", 5 + 2, 5 + 0);
		RoadTile t4 = new RoadTile("D", 5 + 2, 5 + -1);
		RoadTile t5 = new RoadTile("E", 5 + 3, 5 + -1);
		RoadTile t6 = new RoadTile("F", 5 + 3, 5 + -2);
		RoadTile t7 = new RoadTile("G", 5 + 3, 5 + -3);
		RoadTile t8 = new RoadTile("H", 5 + 2, 5 + -3);
		RoadTile t9 = new RoadTile("I", 5 + 2, 5 + -4);

		final RoadTileGrid grid = new RoadTileGrid();
		grid.put(t1);
		grid.put(t2);
		grid.put(t3);
		grid.put(t4);
		grid.put(t5);
		grid.put(t6);
		grid.put(t7);
		grid.put(t8);
		grid.put(t9);

		List<RoadTile> route = new ArrayList<>();
		for(int i = 0; i < 3; i++) {
			route.add(t1);
			route.add(t2);
			route.add(t3);
			route.add(t4);
			route.add(t5);
			route.add(t6);
			route.add(t7);
			route.add(t8);
			route.add(t9); // go back
			route.add(t8);
			route.add(t7);
			route.add(t6);
			route.add(t5);
			route.add(t4);
			route.add(t3);
			route.add(t2);
		}

		final RoadJourney journey = new RoadJourney(route);
		final RoadJourneyVehicle stepper1 = new RoadJourneyVehicle(journey, 5);
		final RoadJourneyVehicle stepper2 = new RoadJourneyVehicle(journey, 5);

		//

		JPanel canvas = new JPanel() {
			private int delay = 100;

			Vec2 bus1a = new Vec2();
			Vec2 bus1b = new Vec2();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				if(false) {
					for(int y = 0; y < 20; y++) {
						for(int x = 0; x < 20; x++) {
							RoadTile tile = grid.get(x, y);
							if(tile == null)
								continue;
							drawRoadTile(g, tile);
						}
					}

					drawVehicleHead(g, stepper1);
					drawVehicleHead(g, stepper2);
				}
				else {
					for(int y = 0; y < 20; y++) {
						for(int x = 0; x < 20; x++) {
							RoadTile tile = grid.get(x, y);
							if(tile == null)
								continue;
							drawRoadTile2(g, tile);
						}
					}
				}

				{
					Vec2 head1 = stepper1.head().getCoords();
					Vec2 diff = new Vec2();

					diff.x = head1.x - bus1a.x;
					diff.y = head1.y - bus1a.y;
					diff.x *= 0.05f;
					diff.y *= 0.05f;
					bus1a.x += diff.x;
					bus1a.y += diff.y;

					for(int i = 0; true; i++) {
						float dist = Vec2.distance(bus1a, bus1b);
						float len = 0.50f;
						float err = dist - len;
						if(err > -0.005f && err < +0.005f) {
							System.out.println(i);
							break;
						}

						diff.x = bus1a.x - bus1b.x;
						diff.y = bus1a.y - bus1b.y;
						diff.x *= 0.05f * Float.compare(dist, len);
						diff.y *= 0.05f * Float.compare(dist, len);
						bus1b.x += diff.x;
						bus1b.y += diff.y;
					}

					int scale = 64;
					int r = 5;
					g.setColor(Color.BLUE);
					g.fillOval((int) (bus1a.x * scale) - r, (int) (bus1a.y * scale) - r, r * 2, r * 2);
					g.fillOval((int) (bus1b.x * scale) - r, (int) (bus1b.y * scale) - r, r * 2, r * 2);
				}

				try {
					Thread.sleep(10);
				}
				catch (Exception exc) {

				}

				if(--delay % 20 == 0) {
					stepper1.step();
					//if(delay < 0)
					stepper2.step();
				}

				this.repaint();
			}

			private void drawRoadTile2(Graphics g, RoadTile tile) {
				for(int y = 0; y < 4; y++) {
					for(int x = 0; x < 4; x++) {
						Color c = Color.GRAY;
						g.setColor(c);
						g.fillRect(tile.x * 64 + x * 16, tile.y * 64 + y * 16, 16, 16);
					}
				}
			}

			private void drawRoadTile(Graphics g, RoadTile tile) {
				for(int y = 0; y < 4; y++) {
					for(int x = 0; x < 4; x++) {
						Color c;
						int bit = RoadTile.coordsToBit(x, y);
						if(!tile.isEmpty(bit) && !tile.isEnterAllowed(bit)) {
							c = Color.ORANGE;
						}
						else if(!tile.isEmpty(bit)) {
							c = Color.GREEN;
						}
						else if(!tile.isEnterAllowed(bit)) {
							c = Color.RED;
						}
						else {
							c = Color.GRAY;
						}

						g.setColor(c);
						g.fillRect(tile.x * 64 + x * 16, tile.y * 64 + y * 16, 16, 16);
					}
				}
			}

			private void drawVehicleHead(Graphics g, RoadJourneyVehicle vehicle) {
				RoadTile tile = vehicle.head().getTile();
				int bit = vehicle.head().getTileBit();
				int x = RoadTile.bitToX(bit);
				int y = RoadTile.bitToY(bit);

				int scale = 64;
				g.setColor(Color.WHITE);
				g.fillRect(tile.x * scale + x * scale / 4, tile.y * scale + y * scale / 4, scale / 4, scale / 4);

				Vec2 coords = vehicle.head().getCoords();
				int r = 5;
				g.setColor(Color.BLACK);
				g.fillOval((int) (coords.x * scale) - r, (int) (coords.y * scale) - r, r * 2, r * 2);
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(800, 600);
			}
		};

		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				RoadTile t = grid.get(5 + 3, 5 + -1);
				t.setEnterAllowed(14, !t.isEnterAllowed(14));
			}
		});

		JFrame frame = new JFrame();
		frame.setTitle("RoadGrid");
		frame.getContentPane().add(canvas);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}
