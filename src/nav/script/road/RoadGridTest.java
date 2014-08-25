package nav.script.road;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import nav.script.road.RoadJourneyStepper.State;
import nav.script.road.verlet.Vec3;
import nav.script.road.verlet.VerletParticle;
import nav.script.road.verlet.VerletSpring;
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
		final RoadJourneyVehicle stepper1 = new RoadJourneyVehicle(journey, 5 + 3);
		final RoadJourneyVehicle stepper2 = new RoadJourneyVehicle(journey, 5 + 3);
		final RoadJourneyVehicle stepper3 = new RoadJourneyVehicle(journey, 5 + 3);

		final float[] maxSubTilesPerStep = new float[] { 0.060f, 0.060f, 0.060f };
		final float[] minSubTilesPerStep = new float[] { 0.010f, 0.010f, 0.010f };
		final float[] accSubTilesPerStep = new float[] { 0.002f, 0.003f, 0.004f };
		stepper1.subTilesPerStep = minSubTilesPerStep[0];
		stepper2.subTilesPerStep = minSubTilesPerStep[1];
		stepper3.subTilesPerStep = minSubTilesPerStep[2];

		//

		class VerletVehicle {
			private final RoadJourneyVehicle vehicle;
			List<VerletParticle> roadNodes = new ArrayList<>();
			List<VerletParticle> swingNodes = new ArrayList<>();
			List<VerletParticle> vehicleNodes = new ArrayList<>();

			List<VerletSpring> roadSwingSprings = new ArrayList<>();
			List<VerletSpring> swingVehicleSprings = new ArrayList<>();
			List<VerletSpring> vehicleVehicleSprings = new ArrayList<>();

			public VerletVehicle(int segmentCount, RoadJourneyVehicle vehicle) {
				this.vehicle = vehicle;

				float x = 0;

				for(int i = 0; i < segmentCount + 1; i++) {
					VerletParticle roadNode = new VerletParticle();
					roadNode.invWeight = 0.00f;
					roadNode.setPosition(x += 1, 0, 0);
					roadNodes.add(roadNode);

					VerletParticle swingNode = new VerletParticle();
					swingNode.invWeight = 0.01f;
					swingNode.setPosition(x += 1, 0, 0);
					swingNodes.add(swingNode);

					VerletParticle vehicleNode = new VerletParticle();
					vehicleNode.invWeight = 1.00f;
					vehicleNode.setPosition(x += 1, 0, 0);
					vehicleNodes.add(vehicleNode);

					VerletSpring roadSwing = new VerletSpring(roadNode, swingNode);
					roadSwing.how = VerletSpring.ENFORCE_FIXED_LENGTH;
					roadSwing.len = 0.025f;
					roadSwing.stf = 0.01f + 0.03f;
					roadSwingSprings.add(roadSwing);

					VerletSpring swingVehicle = new VerletSpring(swingNode, vehicleNode);
					swingVehicle.how = VerletSpring.ENFORCE_FIXED_LENGTH;
					swingVehicle.len = 0.025f;
					swingVehicle.stf = 0.02f + 0.02f;
					swingVehicleSprings.add(swingVehicle);

					if(i > 0) {
						VerletParticle prevVehicleNode = vehicleNodes.get(vehicleNodes.size() - 1 - 1);
						VerletSpring vehicleVehicle = new VerletSpring(prevVehicleNode, vehicleNode);
						vehicleVehicle.how = VerletSpring.ENFORCE_FIXED_LENGTH;
						vehicleVehicle.len = 0.5f;
						vehicleVehicle.stf = 1.0f;
						vehicleVehicleSprings.add(vehicleVehicle);
					}
				}
			}

			public void setVelocity(float subTilesPerStep) {
				for(VerletSpring vs : roadSwingSprings)
					vs.stf = subTilesPerStep * 0.5f;
				for(VerletSpring vs : swingVehicleSprings)
					vs.stf = subTilesPerStep * 1.0f;
			}

			public void tick() {
				final float dragFactor = 0.66f;

				int h = 0;
				for(VerletParticle vp : roadNodes) {
					vp.tick();
					if(vehicle.hasHistory(h)) {
						Vec2 prev = vehicle.getHistory(h);
						vp.setPosition(prev.x, prev.y, 0.0f);
						vp.setVelocity(0.0f, 0.0f, 0.0f);
					}
					h += 2;
				}

				for(VerletParticle vp : swingNodes)
					vp.tick();
				for(VerletParticle vp : vehicleNodes)
					vp.tick();

				for(VerletParticle vp : roadNodes)
					vp.mulVelocity(dragFactor);
				for(VerletParticle vp : swingNodes)
					vp.mulVelocity(dragFactor);
				for(VerletParticle vp : vehicleNodes)
					vp.mulVelocity(dragFactor);

				for(VerletSpring vs : roadSwingSprings)
					vs.tick();
				for(VerletSpring vs : swingVehicleSprings)
					vs.tick();
				for(VerletSpring vs : vehicleVehicleSprings)
					vs.tick();

				for(VerletParticle vp : roadNodes)
					vp.mulVelocity(dragFactor);
				for(VerletParticle vp : swingNodes)
					vp.mulVelocity(dragFactor);
				for(VerletParticle vp : vehicleNodes)
					vp.mulVelocity(dragFactor);
			}
		}

		JPanel canvas = new JPanel() {
			VerletVehicle vv1 = new VerletVehicle(3, stepper1);
			VerletVehicle vv2 = new VerletVehicle(3, stepper2);
			VerletVehicle vv3 = new VerletVehicle(3, stepper3);

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
					drawVehicleHead(g, stepper3);
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

					State state1 = stepper1.step();
					State state2 = stepper2.step();
					State state3 = stepper3.step();

					if(state1 == State.MOVING)
						stepper1.subTilesPerStep += accSubTilesPerStep[0];
					else if(state1 == State.BLOCKED)
						stepper1.subTilesPerStep *= 0.75f;
					stepper1.subTilesPerStep = Math.max(minSubTilesPerStep[0], stepper1.subTilesPerStep);
					stepper1.subTilesPerStep = Math.min(maxSubTilesPerStep[0], stepper1.subTilesPerStep);

					if(state2 == State.MOVING)
						stepper2.subTilesPerStep += accSubTilesPerStep[1];
					else if(state2 == State.BLOCKED)
						stepper2.subTilesPerStep *= 0.75f;
					stepper2.subTilesPerStep = Math.max(minSubTilesPerStep[1], stepper2.subTilesPerStep);
					stepper2.subTilesPerStep = Math.min(maxSubTilesPerStep[1], stepper2.subTilesPerStep);

					if(state3 == State.MOVING)
						stepper3.subTilesPerStep += accSubTilesPerStep[2];
					else if(state3 == State.BLOCKED)
						stepper3.subTilesPerStep *= 0.75f;
					stepper3.subTilesPerStep = Math.max(minSubTilesPerStep[2], stepper3.subTilesPerStep);
					stepper3.subTilesPerStep = Math.min(maxSubTilesPerStep[2], stepper3.subTilesPerStep);

					vv1.setVelocity(stepper1.subTilesPerStep);
					vv2.setVelocity(stepper2.subTilesPerStep);
					vv3.setVelocity(stepper3.subTilesPerStep);

					vv1.tick();
					vv2.tick();
					vv3.tick();

					int scale = 64;
					float hw = 5.0f;

					g.setColor(Color.BLUE);
					for(VerletVehicle vv : new VerletVehicle[] { vv1, vv2, vv3 }) {
						Vec3 p1 = vv.vehicleNodes.get(0).now;
						Vec3 p2 = vv.vehicleNodes.get(1).now;
						Vec3 p3 = vv.vehicleNodes.get(2).now;
						Vec3 p4 = vv.vehicleNodes.get(3).now;

						this.drawBus(g, p1, p2, scale, hw);
						this.drawBus(g, p2, p3, scale, hw);
						this.drawBus(g, p3, p4, scale, hw);

						if(false) {
							for(VerletSpring vs : vv.roadSwingSprings) {
								this.drawSpring(g, vs, scale);
							}
							for(VerletSpring vs : vv.swingVehicleSprings) {
								this.drawSpring(g, vs, scale);
							}
							for(VerletSpring vs : vv.vehicleVehicleSprings) {
								this.drawSpring(g, vs, scale);
							}
						}
					}
				}

				try {
					Thread.sleep(10);
				}
				catch (Exception exc) {

				}

				this.repaint();
			}

			private void drawSpring(Graphics g, VerletSpring spring, float scale) {
				Vec2 p1 = new Vec2();
				Vec2 p2 = new Vec2();
				p1.x = (int) (spring.a.now.x * scale);
				p1.y = (int) (spring.a.now.y * scale);
				p2.x = (int) (spring.b.now.x * scale);
				p2.y = (int) (spring.b.now.y * scale);
				g.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
			}

			private void drawBus(Graphics g, Vec3 head, Vec3 tail, float scale, float r) {
				float nx, ny;
				{
					float dx = head.x - tail.x;
					float dy = head.y - tail.y;
					float dd = (float) Math.sqrt(dx * dx + dy * dy);
					nx = dx / dd;
					ny = dy / dd;
				}

				float dx = ny;
				float dy = -nx;

				Vec2 p1 = new Vec2();
				Vec2 p2 = new Vec2();
				Vec2 p3 = new Vec2();
				Vec2 p4 = new Vec2();
				p1.x = (int) (head.x * scale + dx * +r);
				p1.y = (int) (head.y * scale + dy * +r);
				p2.x = (int) (head.x * scale + dx * -r);
				p2.y = (int) (head.y * scale + dy * -r);
				p3.x = (int) (tail.x * scale + dx * +r);
				p3.y = (int) (tail.y * scale + dy * +r);
				p4.x = (int) (tail.x * scale + dx * -r);
				p4.y = (int) (tail.y * scale + dy * -r);

				Polygon p = new Polygon();
				p.addPoint((int) p1.x, (int) p1.y);
				p.addPoint((int) p2.x, (int) p2.y);
				p.addPoint((int) p4.x, (int) p4.y);
				p.addPoint((int) p3.x, (int) p3.y);
				g.fillPolygon(p);
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
