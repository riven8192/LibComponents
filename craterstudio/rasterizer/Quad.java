/*
 * Created on 26 feb 2010
 */

package craterstudio.rasterizer;

import java.util.ArrayList;
import java.util.List;

import craterstudio.math.VecMath;

public class Quad {
	public Vertex a, b, c, d;

	public Quad() {
		this(null, null, null, null);
	}

	public Quad(Vertex a, Vertex b, Vertex c, Vertex d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	public Quad(Quad abcd) {
		this.a = abcd.a.copy();
		this.b = abcd.b.copy();
		this.c = abcd.c.copy();
		this.d = abcd.d.copy();
	}

	//

	public double area() {
		return new Triangle(b, c, d).area() + new Triangle(d, a, b).area();
	}

	public double maxLength() {
		return Math.sqrt(this.maxLength());
	}

	public double squaredMaxLength() {
		float ab = a.position.squaredDistance(b.position);
		float bc = b.position.squaredDistance(c.position);
		float p1 = Math.max(ab, bc);

		float cd = c.position.squaredDistance(d.position);
		float da = d.position.squaredDistance(a.position);
		float p2 = Math.max(cd, da);

		float ac = a.position.squaredDistance(c.position);
		float bd = b.position.squaredDistance(d.position);
		float p3 = Math.max(ac, bd);

		return Math.max(p1, Math.max(p2, p3));
	}

	public static void asTriangles(List<Quad> quads, List<Triangle> tris) {
		for (Quad quad : quads) {
			quad.asTriangles(tris);
		}
	}

	public void asTriangles(List<Triangle> tris) {
		tris.add(new Triangle(b, c, d));
		tris.add(new Triangle(d, a, b));
	}

	//

	public static List<Quad> splitModelUsingProjection(Quad view, Quad model, double maxLength) {
		final double squaredMaxLength = maxLength * maxLength;

		List<Quad> modelFinal = new ArrayList<Quad>();

		List<Quad> viewSplit = split(view);
		List<Quad> modelSplit = split(model);

		while (!viewSplit.isEmpty()) {
			List<Quad> viewNext = new ArrayList<Quad>();
			List<Quad> modelNext = new ArrayList<Quad>();

			for (int i = 0; i < viewSplit.size(); i++) {
				Quad viewQuad = viewSplit.get(i);
				Quad modelQuad = modelSplit.get(i);

				if (viewQuad.squaredMaxLength() < squaredMaxLength) {
					modelFinal.add(modelQuad);
				} else {
					viewNext.addAll(split(viewQuad));
					modelNext.addAll(split(modelQuad));
				}
			}

			viewSplit = viewNext;
			modelSplit = modelNext;
		}

		return modelFinal;
	}

	public static List<Quad> split(Quad quad) {
		List<Quad> one = new ArrayList<Quad>();
		one.add(quad);
		return split(one);
	}

	public static List<Quad> split(List<Quad> src) {
		List<Quad> dst = new ArrayList<Quad>();
		Quad.split(src, dst);
		return dst;
	}

	public static void split(List<Quad> src, List<Quad> dst) {
		for (Quad quad : src) {
			quad.splitIntoList(dst);
		}
	}

	public void splitIntoList(List<Quad> list) {
		Vertex ab = new Vertex();
		Vertex bc = new Vertex();
		Vertex cd = new Vertex();
		Vertex da = new Vertex();
		Vertex m0 = new Vertex();
		Vertex m1 = new Vertex();
		Vertex m2 = new Vertex();

		//

		VecMath.lerp(0.5f, a.position, b.position, ab.position);
		VecMath.lerp(0.5f, b.position, c.position, bc.position);
		VecMath.lerp(0.5f, c.position, d.position, cd.position);
		VecMath.lerp(0.5f, d.position, a.position, da.position);
		VecMath.lerp(0.5f, a.position, c.position, m1.position);
		VecMath.lerp(0.5f, b.position, d.position, m2.position);
		VecMath.lerp(0.5f, m1.position, m2.position, m0.position);

		VecMath.lerp(0.5f, a.color, b.color, ab.color);
		VecMath.lerp(0.5f, b.color, c.color, bc.color);
		VecMath.lerp(0.5f, c.color, d.color, cd.color);
		VecMath.lerp(0.5f, d.color, a.color, da.color);
		VecMath.lerp(0.5f, a.color, c.color, m1.color);
		VecMath.lerp(0.5f, b.color, d.color, m2.color);
		VecMath.lerp(0.5f, m1.color, m2.color, m0.color);

		VecMath.lerp(0.5f, a.texcoord, b.texcoord, ab.texcoord);
		VecMath.lerp(0.5f, b.texcoord, c.texcoord, bc.texcoord);
		VecMath.lerp(0.5f, c.texcoord, d.texcoord, cd.texcoord);
		VecMath.lerp(0.5f, d.texcoord, a.texcoord, da.texcoord);
		VecMath.lerp(0.5f, a.texcoord, c.texcoord, m1.texcoord);
		VecMath.lerp(0.5f, b.texcoord, d.texcoord, m2.texcoord);
		VecMath.lerp(0.5f, m1.texcoord, m2.texcoord, m0.texcoord);

		//

		list.add(new Quad(a, ab, m0, da));
		list.add(new Quad(ab, b, bc, m0));
		list.add(new Quad(m0, bc, c, cd));
		list.add(new Quad(da, m0, cd, d));
	}
}
