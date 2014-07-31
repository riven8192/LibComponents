package nav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import nav.model.Bus;
import nav.model.Game;
import nav.model.Passenger;
import nav.model.Route;
import nav.model.Station;
import nav.model.TravelPlan;
import nav.model.World;
import nav.model.TravelPlan.Travel;
import nav.util.Clock;
import nav.util.Vec2;

public class NavMain {
	public static void main(String[] args) throws InterruptedException {
		Station a = new Station("1", new Vec2(100, 0));
		Station b = new Station("2", new Vec2(0, 100));
		Station c = new Station("3", new Vec2(0, 0));

		Passenger p1 = new Passenger("1", b);
		//Passenger p2 = new Passenger(a);
		//Passenger p3 = new Passenger(b);

		Route route = new Route();
		route.stations.add(a);
		route.stations.add(b);
		route.stations.add(c);

		Bus bus = new Bus("1", route);
		bus.capacity = 100;
		bus.velocity = 10.0f;
		bus.doDepartAt = Clock.millis() + 8000;

		TravelPlan plan = new TravelPlan();
		plan.addTravel(new Travel(route, b, a));
		p1.setTravelPlan(plan);
		p1.start();

		bus.start();

		while (true) {
			Game.tick();

			Thread.sleep(1000);
		}
	}

	public static void main2(String[] args) {
		final int dim = 512;
		final Random rndm = new Random(2304589);

		final World world = null;
		final Route route = null;

		//

		final JPanel canvas = new JPanel() {
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				g.setColor(Color.BLACK);
				for(Station station : world.stations) {
					int x = (int) station.pos.x;
					int y = (int) station.pos.y;
					g.fillOval(x - 3, y - 3, 7, 7);
					g.drawString(station.id + " (" + station.passengers().size() + "p)", x + 5, y - 5);
				}

				g.setColor(Color.BLUE);
				for(int i = 1; i < route.stations.size(); i++) {
					Station s1 = route.stations.get(i - 1);
					Station s2 = route.stations.get(i - 0);

					g.drawLine((int) s1.pos.x, (int) s1.pos.y, (int) s2.pos.x, (int) s2.pos.y);
				}

				g.setColor(Color.RED);
				for(Bus bus : route.buses) {
					int x, y;
					if(bus.departedAt == -1) {
						x = (int) bus.at.pos.x;
						y = (int) bus.at.pos.y;
					}
					else {
						x = (int) ((bus.at.pos.x + bus.to.pos.x) * 0.5f);
						y = (int) ((bus.at.pos.y + bus.to.pos.y) * 0.5f);
					}

					g.fillRect(x - 3, y - 3, 7, 7);
					g.drawString("B (" + bus.passengers().size() + "p)", x + 5, y + 15);
				}
			}

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(dim, dim);
			}
		};
		JFrame frame = new JFrame();
		frame.setTitle("NavMain");
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.setResizable(false);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);

		Timer timer = new Timer(1000 / 60, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Game.tick();
				canvas.repaint();
			}
		});
		timer.start();
	}
}
