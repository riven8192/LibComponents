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
import nav.model.World;

public class NavMain {
	public static void main(String[] args) {

		final int dim = 512;
		final Random rndm = new Random(2304589);

		final World world = new World();
		for (int i = 0; i < 13; i++) {
			Station station = new Station("S" + i);
			station.pos.x = rndm.nextFloat() * dim;
			station.pos.y = rndm.nextFloat() * dim;

			world.stations.add(station);
		}

		for (int i = 0; i < 100; i++) {
			Passenger passenger = new Passenger();
			passenger.home = world.stations.get(rndm.nextInt(world.stations.size()));
			do {
				passenger.work = world.stations.get(rndm.nextInt(world.stations.size()));
			} while (passenger.home == passenger.work);
			passenger.home.enter(passenger);
		}

		final Route route = new Route();
		route.stations.add(world.stations.get(7));
		route.stations.add(world.stations.get(3));
		route.stations.add(world.stations.get(8));
		route.stations.add(world.stations.get(11));
		route.stations.add(world.stations.get(6));
		route.stations.add(world.stations.get(4));
		route.stations.add(world.stations.get(2));
		route.stations.add(world.stations.get(12));
		route.stations.add(world.stations.get(10));
		route.stations.add(world.stations.get(9));
		{
			Bus bus = new Bus(route);
			bus.capacity = 50;
			bus.velocity = 150.0f;// m/s
			bus.onArrive(route.stations.get(0));
		}	{
			Bus bus = new Bus(route);
			bus.capacity = 30;
			bus.velocity = 10.0f;// m/s
			bus.onArrive(route.stations.get(0));
		}

		//

		final JPanel canvas = new JPanel() {
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);

				g.setColor(Color.BLACK);
				for (Station station : world.stations) {
					int x = (int) station.pos.x;
					int y = (int) station.pos.y;
					g.fillOval(x - 3, y - 3, 7, 7);
					g.drawString(station.id + " (" + station.passengers().size() + "p)", x + 5, y - 5);
				}

				g.setColor(Color.BLUE);
				for (int i = 1; i < route.stations.size(); i++) {
					Station s1 = route.stations.get(i - 1);
					Station s2 = route.stations.get(i - 0);

					g.drawLine((int) s1.pos.x, (int) s1.pos.y, (int) s2.pos.x, (int) s2.pos.y);
				}

				g.setColor(Color.RED);
				for (Bus bus : route.buses) {
					int x, y;
					if (bus.departedAt == -1) {
						x = (int) bus.at.pos.x;
						y = (int) bus.at.pos.y;
					} else {
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
