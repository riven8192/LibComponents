/*
 * Created on 9-apr-2007
 */

package craterstudio.treecull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import craterstudio.math.ShapeMath3D;
import craterstudio.math.Sphere;
import craterstudio.math.Vec3;
import craterstudio.math.VecMath;

public class Octtree implements SpatiallyBound {
	private final Octtree parent;
	private final Vec3 min, max;
	private final Sphere bounding;
	private final OcttreeAI ai;
	private final int depth;

	public Octtree(Vec3 origin, float dim, OcttreeAI ai) {
		this(null, origin, new Vec3(origin).add(dim), ai, 0);
	}

	private Octtree(Octtree parent, Vec3 min, Vec3 max, OcttreeAI ai, int depth) {
		this.parent = parent;
		this.min = min;
		this.max = max;
		this.ai = ai;
		this.depth = depth;

		this.bounding = ShapeMath3D.sphereAroundCube(min, max, new Sphere());
	}

	public final Sphere getBoundingSphere() {
		return bounding;
	}

	public final Octtree getParent() {
		return parent;
	}

	public final int getDepth() {
		return depth;
	}

	private Octtree[] children = null;
	private List<SpatiallyBound> contents = new ArrayList<SpatiallyBound>();
	private int uniqueContainedItems = 0;

	public final void split() {
		if (children != null)
			throw new IllegalStateException("cannot split node: already splitted");

		Vec3 halfDim = new Vec3(max).sub(min).mul(0.5f);

		children = new Octtree[8];

		for (int i = 0; i < children.length; i++) {
			int xi = (i >> 0) & 1;
			int yi = (i >> 1) & 1;
			int zi = (i >> 2) & 1;

			Vec3 newMin = new Vec3(halfDim).mul(xi, yi, zi).add(min);
			Vec3 newMax = new Vec3(newMin).add(halfDim);

			children[i] = new Octtree(this, newMin, newMax, ai, this.depth + 1);
		}

		this.uniqueContainedItems = 0;

		for (SpatiallyBound bs : contents)
			this.put(bs);
		contents.clear();
		contents = null;
	}

	public final void merge() {
		if (children == null)
			throw new IllegalStateException("cannot merge node: not splitted");

		contents = new ArrayList<SpatiallyBound>();

		for (int i = 0; i < children.length; i++) {
			Octtree child = children[i];

			if (child.children != null)
				child.merge();

			contents.addAll(child.contents);
			child.contents.clear();
			child.contents = null;
			child = null;
		}

		children = null;
	}

	public final int countContainedItems() {
		int sum = 0;

		if (children != null)
			for (int i = 0; i < children.length; i++)
				sum += children[i].countContainedItems();
		else
			sum += contents.size();

		return sum;
	}

	public final int countUniqueItems() {
		return uniqueContainedItems;
	}

	public final void put(SpatiallyBound item) {
		// System.out.println("put (" + item + ") at (" + item.getBoundingSphere()
		// + ") at depth " + this.depth);

		this.uniqueContainedItems++;

		if (children == null) {
			if (ai.shouldSplit(this)) {
				this.split();
				this.put(item);
			} else {
				// System.out.println("<<**>> PUT ITEM IN TREE [" + this.depth + "]
				// @ " + item.getBoundingSphere().origin);
				item.getBoundingSphere();
				contents.add(item);
			}
		} else {
			for (int i = 0; i < children.length; i++)
				if (Octtree.nodeContainsItem(children[i], item))
					children[i].put(item);
		}
	}

	public final boolean take(SpatiallyBound item) {
		if (children != null) {
			boolean taken = false;
			for (int i = 0; i < children.length; i++)
				if (nodeContainsItem(children[i], item))
					taken |= children[i].take(item);

			if (taken)
				this.uniqueContainedItems--;
			return taken;
		}

		if (nodeContainsItem(this, item)) {
			boolean removed = contents.remove(item);

			if (removed) {
				this.uniqueContainedItems--;

				if (ai.shouldMerge(this))
					this.merge();
			}

			return removed;
		}

		return false;
	}

	public final void children(List<Octtree> fill) {
		if (children != null) {
			for (int i = 0; i < children.length; i++)
				children[i].children(fill);
		} else {
			fill.add(this);
		}
	}

	public final void fetchItems(OcttreeCuller culler, Set<SpatiallyBound> fill, boolean cullIndividuals) {
		switch (culler.feelIntersection(this)) {
			case OcttreeCuller.NOT_VISIBLE:
				return;

			case OcttreeCuller.PARTIALLY_VISIBLE:
				if (children != null) {
					for (int i = 0; i < children.length; i++)
						children[i].fetchItems(culler, fill, cullIndividuals);
				} else {
					if (cullIndividuals) {
						for (int i = 0; i < contents.size(); i++)
							if (culler.feelIntersection(contents.get(i)) != OcttreeCuller.NOT_VISIBLE)
								fill.add(contents.get(i));
					} else {
						// fill.addAll(contents);
						for (int i = 0; i < contents.size(); i++)
							fill.add(contents.get(i));
					}
				}
				return;

			case OcttreeCuller.FULLY_VISIBLE:
				this.fill(fill);
				return;

			default:
				throw new IllegalStateException("unknown intersection returned");
		}
	}

	public final void fetchLeafs(OcttreeCuller culler, Collection<Octtree> fill) {
		switch (culler.feelIntersection(this)) {
			case OcttreeCuller.NOT_VISIBLE:
				return;

			case OcttreeCuller.PARTIALLY_VISIBLE:
			case OcttreeCuller.FULLY_VISIBLE:
				if (children != null) {
					for (int i = 0; i < children.length; i++)
						children[i].fetchLeafs(culler, fill);
				} else {
					fill.add(this);
				}
				return;

			default:
				throw new IllegalStateException("unknown intersection returned");
		}
	}

	public final void fill(Set<SpatiallyBound> fill) {
		if (children != null) {
			for (int i = 0; i < children.length; i++)
				children[i].fill(fill);
		} else {
			// fill.addAll(contents);
			for (int i = 0; i < contents.size(); i++)
				fill.add(contents.get(i));
		}
	}

	public final void ray(Vec3 src, Vec3 dst, List<SpatiallyBound> fill) {
		if (!ShapeMath3D.lineHitsSphere(src, dst, this.bounding)) {
			return;
		}

		if (children != null) {
			for (int i = 0; i < children.length; i++)
				this.ray(src, dst, fill);
		} else {
			fill.addAll(contents);
		}
	}

	private static final boolean nodeContainsItem(Octtree node, SpatiallyBound item) {
		return VecMath.isInRange(node.getBoundingSphere(), item.getBoundingSphere());
	}
}