/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import craterstudio.math.EasyMath;
import craterstudio.misc.ImageUtil;

public class PixelBufferFrame extends Frame {
	public static void main(String[] args) throws Exception {
		final int w = 700 * 1;
		final int h = w * 3 / 4;

		final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		final int[] imageRGB = ImageUtil.accessRasterIntArray(image);
		final PixelBuffer buffer = new PixelBuffer(imageRGB, w);

		final PixelBufferFrame frame = new PixelBufferFrame();
		frame.setTitle("FrameBuffer");
		frame.setLayout(new BorderLayout());

		BufferedImage textureImage = ImageIO.read(new File("M:/somewhere-over-the-rainbow.jpg"));
		textureImage = ImageUtil.copy(textureImage, BufferedImage.TYPE_INT_RGB);
		final Texture texture = new Texture(textureImage);
		texture.createMipmaps(32, true);

		final AccumulationBuffer accum = new AccumulationBuffer(w, h);

		Canvas canvas = new Canvas() {
			@Override
			public void paint(Graphics g) {
				accum.clear(0, 0, 0);

				double rotx = 45 + 15 * 2;
				double roty = 45 + 15 * 2;
				if (this.getMousePosition() != null)
					rotx += this.getMousePosition().x * 0.25f;
				if (this.getMousePosition() != null)
					roty -= this.getMousePosition().y * 0.05f;

				int frames = 25 * 2 + 1;
				long t0_ = System.nanoTime();
				for (int m = 0; m < frames; m++) {
					Quad model = new Quad();

					model.a = new Vertex();
					model.b = new Vertex();
					model.c = new Vertex();
					model.d = new Vertex();

					float diff = -0.125f / frames;

					model.a.position.load(+1.2f + m * diff, -0.1f, +0.3f);
					model.b.position.load(+0.3f, +0.3f, +0.4f);
					model.c.position.load(+0.7f, +0.4f - m * diff, +0.5f);
					model.d.position.load(+0.6f, -0.3f, +0.7f + m * diff);

					model.a.color.load(1.0f, 0.5f, 0.0f);
					model.b.color.load(1.0f, 1.0f, 0.0f);
					model.c.color.load(1.0f, 1.0f, 1.0f);
					model.d.color.load(0.5f, 0.5f, 1.0f);

					model.a.texcoord.load(0.0f, 0.0f);
					model.b.texcoord.load(2.0f, 0.0f);
					model.c.texcoord.load(2.0f, 2.0f);
					model.d.texcoord.load(0.0f, 2.0f);

					//

					buffer.setTexture(null);
					buffer.clearColorBuffer(0xFFFFFF);
					buffer.clearDepthBuffer();
					{
						buffer.setTextureQuality(true);
						buffer.modelView.identity();
						buffer.projection.identity();

						buffer.projection.scale(1.0f, (float) w / h, 1.0f);
						buffer.modelView.translate(-0.0f, -0.8f, -0.0f);

						// buffer.modelView.rotX(rot);
						buffer.modelView.rotY(rotx);
						buffer.modelView.rotZ(roty);
					}

					buffer.setTexture(texture);
					buffer.updateTransform(true);

					List<Triangle> tris = new ArrayList<Triangle>();
					{
						Quad view = new Quad(model);
						buffer.modelViewProjection.transform(view.a.position);
						buffer.modelViewProjection.transform(view.b.position);
						buffer.modelViewProjection.transform(view.c.position);
						buffer.modelViewProjection.transform(view.d.position);

						List<Quad> modelQuads = Quad.splitModelUsingProjection(view, model, 32);
						for (Quad modelQuad : modelQuads) {
							modelQuad.asTriangles(tris);
						}
					}

					{
						for (Triangle tri : tris)
							buffer.renderTriangle(tri);
					}

					if (m <= frames / 2)
						accum.accumulate(buffer, EasyMath.interpolate(m, 0, frames / 2, 0.25f, 1.75f));
					else
						accum.accumulate(buffer, EasyMath.interpolate(m, frames / 2, frames, 1.75f, 0.25f));
				}

				accum.mergeInto(buffer);
				long t1_ = System.nanoTime();
				System.out.println("rendering " + frames + "x took: " + (t1_ - t0_) / 1000000L + "ms");
				System.out.println();

				// draw
				g.drawImage(image, 0, 0, null);

				this.repaint();
			}

			@Override
			public void update(Graphics g) {
				this.paint(g);
			}
		};
		canvas.setPreferredSize(new Dimension(w, h));
		frame.add(canvas, BorderLayout.CENTER);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				frame.dispose();
				System.exit(0);
			}
		});
	}
}
